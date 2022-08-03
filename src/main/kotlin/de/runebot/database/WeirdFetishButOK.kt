package de.runebot.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object UserCollections : Table()
{
    val userId: Column<Long> = long("userId")
    val characterName: Column<String> = text("characterName")
    val seriesName: Column<String> = text("seriesName")
}

object Timers : Table()
{
    val targetTime: Column<Long> = long("targetTime") // time in System.currentTimeMillis()
    val message: Column<String> = text("message")
    val channelId: Column<Long> = long("channelId")
    val messageId: Column<Long> = long("messageId")
}

object Tags : Table()
{
    val name: Column<String> = varchar("name", 50)
    val message: Column<String> = text("message")
    val creatorId: Column<Long> = long("creatorId")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(name)
}