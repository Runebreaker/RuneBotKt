package de.runebot.database

import de.runebot.commands.NumbersCommand.DoujinData
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
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
            SchemaUtils.create(UserCollections, Timers, Tags)
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
                return@transaction Tags.select {
                    Tags.name eq name
                }.map { it[Tags.message] }.firstOrNull()
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
                return@transaction Tags.select {
                    Tags.name eq name
                }.map { it[Tags.creatorId] }.firstOrNull()
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
                return@transaction Tags.select {
                    Tags.creatorId eq ownerID
                }.map { it[Tags.name] }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    private fun checkForTagCreatorAndEntry(name: String, creator: Long): DBResponse
    {
        Tags.select {
            Tags.name eq name
        }.firstOrNull()?.let {
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
    fun addTimer(duration: Duration, message: String, channelId: Long, messageId: Long): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                Timers.insert {
                    it[this.targetTime] = System.currentTimeMillis() + duration.inWholeMilliseconds
                    it[this.message] = message
                    it[this.channelId] = channelId
                    it[this.messageId] = messageId
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun removeTimer(channelId: Long, messageId: Long): DBResponse
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

    fun getAllTimers(): List<TimerEntry>
    {
        try
        {
            return transaction(mainDB) {
                return@transaction Timers.selectAll().map {
                    TimerEntry(it[Timers.targetTime], it[Timers.message], it[Timers.channelId], it[Timers.messageId])
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun removeAllTimers(): DBResponse
    {
        try
        {
            return transaction(mainDB) {
                Timers.deleteAll()
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    data class TimerEntry(val targetTime: Long, val message: String, val channelId: Long, val messageId: Long)
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
                UserCollections.select {
                    UserCollections.userId eq userId
                }.forEach { totalResults.add(Pair(it[UserCollections.characterName], it[UserCollections.seriesName])) }
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
                UserCollections.select {
                    UserCollections.characterName eq characterName
                }.forEach { totalResults.add(Pair(it[UserCollections.userId], it[UserCollections.seriesName])) }
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
                UserCollections.select {
                    UserCollections.seriesName eq seriesName
                }.forEach { totalResults.add(Pair(it[UserCollections.userId], it[UserCollections.characterName])) }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
        }
        return totalResults
    }

    private fun checkForCollectionCreatorAndEntry(userId: Long, characterName: String, seriesName: String): DBResponse
    {
        UserCollections.select {
            (UserCollections.characterName eq characterName) and (UserCollections.seriesName eq seriesName)
        }.firstOrNull()?.let {
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
                return@transaction Doujins.select {
                    Doujins.number eq number
                }.firstOrNull()?.let {
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