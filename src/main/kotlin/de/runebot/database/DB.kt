package de.runebot.database

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.Path

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
                val check = checkForCreatorAndEntry(name, creator)
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
                val check = checkForCreatorAndEntry(name, creator)
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

    private fun checkForCreatorAndEntry(name: String, creator: Long): DBResponse
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
}