package de.runebot.database

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.Path
import kotlin.time.Duration

object DB
{
    init
    {
        val pathToDB = DB::class.java.getResource("db.sqlite")?.let { Path(it.path) } ?: Path("db.sqlite") // resources is eh net gut

        Database.connect("jdbc:sqlite:$pathToDB")

        transaction {
            SchemaUtils.create(UserCollections, Timers, Tags)
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
            return transaction {
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
            return transaction {
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
            return transaction {
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
            return transaction {
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
            return transaction {
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

    fun getTag(name: String): String?
    {
        try
        {
            return transaction {
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
            return transaction {
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

    fun removeAllTimers(): DBResponse
    {
        try
        {
            return transaction {
                Timers.deleteAll()
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }
    //endregion

    //region Collection API
    fun addToCollection(userId: Long, characterName: String, seriesName: String): DBResponse
    {
        try
        {
            return transaction {
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
            return transaction {
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
            return transaction {
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
            transaction {
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
            transaction {
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
            transaction {
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
}