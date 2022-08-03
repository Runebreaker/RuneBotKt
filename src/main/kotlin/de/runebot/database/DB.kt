package de.runebot.database

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
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
    fun storeTag(name: String, message: String, creator: Snowflake?): Boolean
    {
        try
        {
            transaction {
                Tags.insert {
                    it[this.name] = name
                    it[this.message] = message
                    it[this.creatorId] = creator?.value?.toLong() ?: 0
                }
            }
        } catch (e: ExposedSQLException)
        {
            return false
        }

        return true
    }

    fun getTag(name: String): String?
    {
        return transaction {
            return@transaction Tags.select {
                Tags.name eq name
            }.map { it[Tags.message] }.firstOrNull()
        }
    }
}