package de.runebot.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
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

//endregion

//region Puzzle DB

object Puzzles : Table()
{
    val puzzleId: Column<Int> = integer("puzzleId").autoIncrement()
    val creatorId: Column<Long> = long("userId")
    val approved: Column<Boolean> = bool("approved")
    val name: Column<String> = text("name")
    val description: Column<String> = text("description")
    val difficulty: Column<Int> = integer("difficulty")
    val maxAttempts: Column<Int> = integer("maxAttempts")
    val rewardType: Column<String> = text("rewardType")
    val rewardAmount: Column<Int> = integer("rewardAmount")
    val puzzleDetails: Column<String> = text("puzzleDetails")
    val puzzleType: Column<Int> = integer("puzzleType")
    val puzzleTips: Column<String?> = text("puzzleExplanation").nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(puzzleId)
}

object PuzzleStatsByUser : Table()
{
    val userId: Column<Long> = long("userId").references(UserStatsOverall.userId)
    val puzzleId: Column<Int> = integer("puzzleId").references(Puzzles.puzzleId)
    val attempts: Column<Int> = integer("attempts")
    val solved: Column<Boolean> = bool("solved")
    val time: Column<Int> = integer("time")

    override val primaryKey: PrimaryKey = PrimaryKey(userId, puzzleId)
}

object UserStatsOverall : Table()
{
    val userId: Column<Long> = long("userId")
    val activePuzzle: Column<Int?> = integer("activePuzzle").references(Puzzles.puzzleId).nullable()
    val solvedAmount: Column<Int> = integer("solvedAmount")
    val rewardPoints: Column<Int> = integer("rewardPoints")

    override val primaryKey: PrimaryKey = PrimaryKey(userId)
}

//endregion