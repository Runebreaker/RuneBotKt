package de.runebot.database

import de.runebot.commands.NumbersCommand.DoujinData
import dev.kord.common.toMessageFormat
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import kotlin.io.path.Path
import kotlin.time.Duration

object DB
{
    private val serializer = Json
    private var mainDB: Database
    private var doujinDB: Database
    private var ghtTagDB: Database

    init
    {
        val pathToMainDB = Path("dbs/db.sqlite")
        val pathToDoujinDB = Path("dbs/doujinInfos.sqlite")
        val pathToGhtTags = Path("dbs/ghtTagDB.sqlite")

        mainDB = Database.connect("jdbc:sqlite:$pathToMainDB")
        doujinDB = Database.connect("jdbc:sqlite:$pathToDoujinDB")
        ghtTagDB = Database.connect("jdbc:sqlite:$pathToGhtTags")

        transaction(mainDB) {
            SchemaUtils.create(UserCollections, Timers, Tags, TimersV2)
        }

        transaction(doujinDB) {
            SchemaUtils.create(Doujins)
        }

        transaction(ghtTagDB) {
            SchemaUtils.create(Tags)
        }
    }

    //region Tags API
    /**
     * @return if insert is successful
     */
    fun storeTag(name: String, message: String, creator: Long): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                Tags.insert {
                    it[this.name] = name
                    it[this.message] = message
                    it[this.creatorId] = creator
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    /**
     * @return if update is successful
     */
    fun updateTag(name: String, message: String, creator: Long): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                Tags.update({ Tags.name eq name }) {
                    it[this.message] = message
                    it[this.creatorId] = creator
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    /**
     * @return if update is successful
     */
    fun updateTagIfOwner(name: String, message: String, creator: Long): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                val check = checkForTagCreatorAndEntry(name, creator)
                if (check != DBResponse.SUCCESS) return@transaction check
                Tags.update({ (Tags.name eq name) and (Tags.creatorId eq creator) }) {
                    it[this.message] = message
                    it[this.creatorId] = creator
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    /**
     * @return if delete is successful
     */
    fun deleteTag(name: String): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                Tags.deleteWhere {
                    Tags.name eq name
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    /**
     * @return if delete is successful
     */
    fun deleteTagIfOwner(name: String, creator: Long): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                val check = checkForTagCreatorAndEntry(name, creator)
                if (check != DBResponse.SUCCESS) return@transaction check
                Tags.deleteWhere {
                    Tags.name eq name
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    fun getTag(name: String, fromGhtTags: Boolean = false): String?
    {
        try
        {
            return transaction(if (fromGhtTags) ghtTagDB else mainDB) {
                return@transaction Tags.selectAll().where { Tags.name eq name }.map { it[Tags.message] }.firstOrNull()
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    fun getTagOwnerId(name: String): Long?
    {
        try
        {
            return transaction(mainDB) {
                return@transaction Tags.selectAll().where { Tags.name eq name }.map { it[Tags.creatorId] }.firstOrNull()
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    fun getTagsOfOwner(ownerID: Long): List<String>?
    {
        try
        {
            return transaction(mainDB) {
                return@transaction Tags.selectAll().where { Tags.creatorId eq ownerID }.map { it[Tags.name] }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    private fun checkForTagCreatorAndEntry(name: String, creator: Long): DBResponse
    {
        Tags.selectAll().where { Tags.name eq name }.firstOrNull()?.let {
            if (it[Tags.creatorId] != creator)
            {
                // IDs do not match
                return DBResponse.WRONG_USER
            }
        } ?: return DBResponse.MISSING_ENTRY

        return DBResponse.SUCCESS
    }
    //endregion

    //region Timer API

    /**
     * @return inserted Timer Entry if successful, null if not
     */
    fun addTimer(creatorId: ULong, targetTime: Instant, message: String): TimerEntry?
    {
        try
        {
            return transaction(mainDB) {
                return@transaction TimersV2.insert {
                    it[this.creatorId] = creatorId
                    it[this.targetTime] = targetTime.toJavaInstant()
                    it[this.message] = message
                    it[this.subscriberIds] = serializer.encodeToString(listOf(creatorId)) // by default, only the creator is subscribed
                }.resultedValues?.firstNotNullOfOrNull {
                    TimerEntry.from(it)
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    fun addTimer(creatorId: ULong, duration: Duration, message: String): TimerEntry?
    {
        return addTimer(creatorId, Clock.System.now() + duration, message)
    }

    fun getTimer(id: Int): TimerEntry?
    {
        try
        {
            return transaction(mainDB) {
                return@transaction TimersV2.selectAll().where {
                    TimersV2.id eq id
                }.firstNotNullOfOrNull {
                    TimerEntry.from(it)
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    /**
     * @return what TimerEntry was subscribed to or null if error
     */
    fun subscribeToTimer(id: Int, userId: ULong): TimerEntry?
    {
        val timer = getTimer(id) ?: return null

        try
        {
            transaction(mainDB) {
                TimersV2.update(where = {
                    TimersV2.id eq id
                }) {
                    it[this.subscriberIds] = serializer.encodeToString(timer.subscriberIds.toMutableList().add(userId))
                }
            }
            return timer
        } catch (e: Exception)
        {
            e.printStackTrace()
            return null
        }
    }

    fun removeTimer(id: Int)
    {
        try
        {
            transaction(mainDB) {
                TimersV2.deleteWhere {
                    TimersV2.id eq id
                }
            }
        } catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    fun removeOldTimer(channelId: Long, messageId: Long): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                Timers.deleteWhere {
                    (Timers.messageId eq messageId) and (Timers.channelId eq channelId)
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun getTimersForCreator(creatorId: ULong): List<TimerEntry>
    {
        try
        {
            return transaction(mainDB) {
                return@transaction TimersV2.selectAll().where {
                    TimersV2.creatorId eq creatorId
                }.mapNotNull {
                    TimerEntry.from(it)
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getAllTimers(): List<TimerEntry>
    {
        try
        {
            return transaction(mainDB) {
                return@transaction TimersV2.selectAll().mapNotNull {
                    TimerEntry.from(it)
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getAllOldTimers(): List<OldTimerEntry>
    {
        try
        {
            return transaction(mainDB) {
                return@transaction Timers.selectAll().map {
                    OldTimerEntry(it[Timers.targetTime], it[Timers.message], it[Timers.channelId], it[Timers.messageId])
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return emptyList()
        }
    }

    data class TimerEntry(val id: Int, val creatorId: ULong, val targetTime: Instant, val message: String, val subscriberIds: List<ULong>)
    {
        override fun toString(): String
        {
            return "$id: ${targetTime.toMessageFormat()} $message"
        }

        companion object
        {
            fun from(resultRow: ResultRow): TimerEntry?
            {
                return try
                {
                    TimerEntry(
                        resultRow[TimersV2.id],
                        resultRow[TimersV2.creatorId],
                        resultRow[TimersV2.targetTime].toKotlinInstant(),
                        resultRow[TimersV2.message],
                        serializer.decodeFromString(resultRow[TimersV2.subscriberIds])
                    )
                } catch (e: Exception)
                {
                    return null
                }
            }
        }
    }

    data class OldTimerEntry(val targetTime: Long, val message: String, val channelId: Long, val messageId: Long)
    //endregion

    //region Collection API
    fun addToCollection(userId: Long, characterName: String, seriesName: String): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                UserCollections.insert {
                    it[this.userId] = userId
                    it[this.characterName] = characterName
                    it[this.seriesName] = seriesName
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun removeFromCollection(userId: Long, characterName: String, seriesName: String): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                UserCollections.deleteWhere {
                    (UserCollections.characterName eq characterName) and (UserCollections.seriesName eq seriesName)
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun removeFromCollectionIfOwner(userId: Long, characterName: String, seriesName: String): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                val check = checkForCollectionCreatorAndEntry(userId, characterName, seriesName)
                if (check != DBResponse.SUCCESS) return@transaction check
                UserCollections.deleteWhere {
                    (UserCollections.characterName eq characterName) and (UserCollections.seriesName eq seriesName)
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun getAllFromCollection(userId: Long): List<Pair<String, String>>
    {
        val totalResults = mutableListOf<Pair<String, String>>()
        try
        {
            transaction(mainDB) {
                UserCollections.selectAll().where { UserCollections.userId eq userId }
                    .forEach { totalResults.add(Pair(it[UserCollections.characterName], it[UserCollections.seriesName])) }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
        }
        return totalResults
    }

    fun searchCollectionsByCharacter(characterName: String): List<Pair<Long, String>>
    {
        val totalResults = mutableListOf<Pair<Long, String>>()
        try
        {
            transaction(mainDB) {
                UserCollections.selectAll().where { UserCollections.characterName eq characterName }
                    .forEach { totalResults.add(Pair(it[UserCollections.userId], it[UserCollections.seriesName])) }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
        }
        return totalResults
    }

    fun searchCollectionsBySeries(seriesName: String): List<Pair<Long, String>>
    {
        val totalResults = mutableListOf<Pair<Long, String>>()
        try
        {
            transaction(mainDB) {
                UserCollections.selectAll().where { UserCollections.seriesName eq seriesName }
                    .forEach { totalResults.add(Pair(it[UserCollections.userId], it[UserCollections.characterName])) }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
        }
        return totalResults
    }

    private fun checkForCollectionCreatorAndEntry(userId: Long, characterName: String, seriesName: String): DBResponse
    {
        UserCollections.selectAll().where { (UserCollections.characterName eq characterName) and (UserCollections.seriesName eq seriesName) }.firstOrNull()?.let {
            if (it[UserCollections.userId] != userId)
            {
                // IDs do not match
                return DBResponse.WRONG_USER
            }
        } ?: return DBResponse.MISSING_ENTRY

        return DBResponse.SUCCESS
    }
    //endregion

    //region Doujin API

    fun storeDoujin(
        number: Int,
        name: String,
        original_name: String,
        parodies: MutableCollection<String>,
        characters: MutableCollection<String>,
        tags: MutableCollection<String>,
        artists: MutableCollection<String>,
        groups: MutableCollection<String>,
        languages: MutableCollection<String>,
        categories: MutableCollection<String>,
        page_number: Int,
        upload_date: LocalDate
    ): DBResponse
    {
        try
        {
            return transaction(doujinDB) {
                Doujins.insert {
                    it[this.number] = number
                    it[this.name] = name
                    it[this.original_name] = original_name
                    it[this.parodies] = serializer.encodeToString(parodies)
                    it[this.characters] = serializer.encodeToString(characters)
                    it[this.tags] = serializer.encodeToString(tags)
                    it[this.artists] = serializer.encodeToString(artists)
                    it[this.groups] = serializer.encodeToString(groups)
                    it[this.languages] = serializer.encodeToString(languages)
                    it[this.categories] = serializer.encodeToString(categories)
                    it[this.page_number] = page_number
                    it[this.upload_date] = upload_date
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun storeDoujin(number: Int, doujin: DoujinData)
    {
        transaction(doujinDB) {
            Doujins.insert {
                it[this.number] = number
                it[this.name] = doujin.name
                it[this.original_name] = doujin.original_name ?: ""
                it[this.parodies] = serializer.encodeToString(doujin.parodies)
                it[this.characters] = serializer.encodeToString(doujin.characters)
                it[this.tags] = serializer.encodeToString(doujin.tags)
                it[this.artists] = serializer.encodeToString(doujin.artists)
                it[this.groups] = serializer.encodeToString(doujin.groups)
                it[this.languages] = serializer.encodeToString(doujin.languages)
                it[this.categories] = serializer.encodeToString(doujin.categories)
                it[this.page_number] = doujin.page_number
                it[this.upload_date] = doujin.upload_date.toJavaLocalDate()
            }
        }
    }

    fun deleteDoujin(number: Int): DBResponse
    {
        try
        {
            return transaction(doujinDB) {
                Doujins.deleteWhere {
                    Doujins.number eq number
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    fun deleteAllDoujins(): DBResponse
    {
        try
        {
            return transaction(doujinDB) {
                Doujins.deleteAll()
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun getDoujin(number: Int): DoujinData?
    {
        try
        {
            return transaction(doujinDB) {
                return@transaction Doujins.selectAll().where { Doujins.number eq number }.firstOrNull()?.let {
                    DoujinData(
                        it[Doujins.name],
                        it[Doujins.original_name],
                        serializer.decodeFromString(it[Doujins.parodies]),
                        serializer.decodeFromString(it[Doujins.characters]),
                        serializer.decodeFromString(it[Doujins.tags]),
                        serializer.decodeFromString(it[Doujins.artists]),
                        serializer.decodeFromString(it[Doujins.groups]),
                        serializer.decodeFromString(it[Doujins.languages]),
                        serializer.decodeFromString(it[Doujins.categories]),
                        it[Doujins.page_number],
                        it[Doujins.upload_date].toKotlinLocalDate()
                    )
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    fun getAllDoujins(): Map<Int, DoujinData>
    {
        return transaction(doujinDB) {
            return@transaction Doujins.selectAll().associate {
                it[Doujins.number] to DoujinData(
                    it[Doujins.name],
                    it[Doujins.original_name],
                    serializer.decodeFromString(it[Doujins.parodies]),
                    serializer.decodeFromString(it[Doujins.characters]),
                    serializer.decodeFromString(it[Doujins.tags]),
                    serializer.decodeFromString(it[Doujins.artists]),
                    serializer.decodeFromString(it[Doujins.groups]),
                    serializer.decodeFromString(it[Doujins.languages]),
                    serializer.decodeFromString(it[Doujins.categories]),
                    it[Doujins.page_number],
                    it[Doujins.upload_date].toKotlinLocalDate()
                )
            }
        }
    }

    //endregion
}