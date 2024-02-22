package de.runebot.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDate

//region Main DB

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

object TimersV2 : Table()
{
    val creatorId = ulong("creatorId")
    val targetTime = timestamp("targetTime")
    val message = text("message")
    val subscriberIds = text("subscriberIds") // list of uLongs as JSON

    override val primaryKey = PrimaryKey(creatorId, targetTime, message)
}

object Tags : Table()
{
    val name: Column<String> = varchar("name", 50)
    val message: Column<String> = text("message")
    val creatorId: Column<Long> = long("creatorId")

    override val primaryKey: PrimaryKey
        get() = PrimaryKey(name)
}

//endregion

//region Doujin DB

object Doujins : Table()
{
    val number: Column<Int> = integer("number")
    var name: Column<String> = text("name")
    var original_name: Column<String> = text("original_name")
    val parodies: Column<String> = text("parodies")
    val characters: Column<String> = text("characters")
    val tags: Column<String> = text("tags")
    val artists: Column<String> = text("artists")
    val groups: Column<String> = text("groups")
    val languages: Column<String> = text("languages")
    val categories: Column<String> = text("categories")
    var page_number: Column<Int> = integer("page_number")
    var upload_date: Column<LocalDate> = date("upload_date")
}