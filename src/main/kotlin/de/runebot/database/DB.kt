package de.runebot.database

import de.runebot.Util
import de.runebot.Util.Puzzle
import de.runebot.commands.NumbersCommand.DoujinData
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
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
    private var puzzleDB: Database

    init
    {
        val pathToMainDB = Path("dbs/db.sqlite")
        val pathToDoujinDB = Path("dbs/doujinInfos.sqlite")
        val pathToGhtTags = Path("dbs/ghtTagDB.sqlite")
        val pathToPuzzleDB = Path("dbs/puzzleDB.sqlite")

        mainDB = Database.connect("jdbc:sqlite:$pathToMainDB")
        doujinDB = Database.connect("jdbc:sqlite:$pathToDoujinDB")
        ghtTagDB = Database.connect("jdbc:sqlite:$pathToGhtTags")
        puzzleDB = Database.connect("jdbc:sqlite:$pathToPuzzleDB")

        transaction(mainDB) {
            SchemaUtils.create(UserCollections, Timers, Tags)
        }

        transaction(doujinDB) {
            SchemaUtils.create(Doujins)
        }

        transaction(ghtTagDB) {
            SchemaUtils.create(Tags)
        }

        transaction(puzzleDB) {
            SchemaUtils.create(Puzzles, PuzzleStatsByUser, UserStatsOverall)
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

    //region Puzzle API
    fun storePuzzle(
        userId: Long,
        name: String,
        description: String,
        difficulty: Int,
        maxAttempts: Int,
        rewardType: Util.PuzzleRewardType,
        rewardAmount: Int,
        puzzleDetails: String,
        puzzleType: Util.PuzzleType,
        hints: List<String>
    ): Int
    {
        try
        {
            return transaction(puzzleDB) {
                Puzzles.insert {
                    it[this.creatorId] = userId
                    it[this.approved] = false
                    it[this.name] = name
                    it[this.description] = description
                    it[this.difficulty] = difficulty
                    it[this.maxAttempts] = maxAttempts
                    it[this.rewardType] = rewardType.id
                    it[this.rewardAmount] = rewardAmount
                    it[this.puzzleDetails] = puzzleDetails
                    it[this.puzzleType] = puzzleType.id
                    it[this.puzzleTips] = hints.joinToString(",")
                }.resultedValues?.firstOrNull()?.get(Puzzles.puzzleId) ?: -1
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return -1
        }
    }

    fun storePuzzleStatsForUser(userId: Long, puzzleId: Int, attempts: Int, solved: Boolean, time: Int): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                PuzzleStatsByUser.insert {
                    it[this.userId] = userId
                    it[this.puzzleId] = puzzleId
                    it[this.attempts] = attempts
                    it[this.solved] = solved
                    it[this.time] = time
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return DBResponse.FAILURE
        }
    }

    fun updatePuzzle(
        puzzleId: Int,
        userId: Long,
        name: String,
        description: String,
        difficulty: Int,
        maxAttempts: Int,
        rewardType: Util.PuzzleRewardType,
        rewardAmount: Int,
        puzzleDetails: String,
        puzzleType: Util.PuzzleType,
        hints: String
    ): Int
    {
        try
        {
            return transaction(puzzleDB) {
                Puzzles.update({ Puzzles.puzzleId eq puzzleId }) {
                    it[this.creatorId] = userId
                    it[this.approved] = false
                    it[this.name] = name
                    it[this.description] = description
                    it[this.difficulty] = difficulty
                    it[this.maxAttempts] = maxAttempts
                    it[this.rewardType] = rewardType.id
                    it[this.rewardAmount] = rewardAmount
                    it[this.puzzleDetails] = puzzleDetails
                    it[this.puzzleType] = puzzleType.id
                    it[this.puzzleTips] = hints
                }
                return@transaction puzzleId
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return -1
        }
    }

    fun markPuzzleAsSolvedForUser(userId: Long, puzzleId: Int): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                val attemptsTaken = getPuzzleStatsForUser(userId, puzzleId)?.first
                val puzzle = getPuzzle(puzzleId)
                PuzzleStatsByUser.update({ (PuzzleStatsByUser.userId eq userId) and (PuzzleStatsByUser.puzzleId eq puzzleId) }) {
                    it[this.solved] = true
                }
                UserStatsOverall.update({ UserStatsOverall.userId eq userId }) {
                    it[this.solvedAmount] = solvedAmount + 1
                    it[this.rewardPoints] =
                        if (puzzle?.rewardType == Util.PuzzleRewardType.PUZZLE_POINTS && attemptsTaken != null && attemptsTaken <= puzzle.maxAttempts) rewardPoints + puzzle.rewardAmount else rewardPoints
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    fun getPuzzleSolvers(puzzleId: Int): List<Long>
    {
        return try
        {
            transaction(puzzleDB) {
                PuzzleStatsByUser.select {
                    PuzzleStatsByUser.puzzleId eq puzzleId and PuzzleStatsByUser.solved
                }.map { it[PuzzleStatsByUser.userId] }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            emptyList()
        }
    }

    fun approvePuzzle(puzzleId: Int): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                Puzzles.update({ Puzzles.puzzleId eq puzzleId }) {
                    it[approved] = true
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    fun getUnapprovedPuzzles(): List<Puzzle>
    {
        return try
        {
            transaction(puzzleDB) {
                Puzzles.select {
                    Puzzles.approved eq false
                }.map {
                    Puzzle.createPuzzle(
                        Util.PuzzleType.fromId(it[Puzzles.puzzleType]),
                        it[Puzzles.puzzleId],
                        it[Puzzles.creatorId],
                        it[Puzzles.puzzleDetails],
                        it[Puzzles.name],
                        it[Puzzles.description],
                        it[Puzzles.difficulty],
                        it[Puzzles.maxAttempts],
                        Util.PuzzleRewardType.fromId(it[Puzzles.rewardType]),
                        it[Puzzles.rewardAmount]
                    )
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            emptyList()
        }
    }

    fun addPuzzleAttemptForUser(userId: Long, puzzleId: Int): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                PuzzleStatsByUser.update({ (PuzzleStatsByUser.userId eq userId) and (PuzzleStatsByUser.puzzleId eq puzzleId) }) {
                    with(SqlExpressionBuilder) {
                        it.update(attempts, attempts + 1)
                    }
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    fun setPuzzleTimeForUser(userId: Long, puzzleId: Int, time: Int): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                PuzzleStatsByUser.update({ (PuzzleStatsByUser.userId eq userId) and (PuzzleStatsByUser.puzzleId eq puzzleId) }) {
                    it[this.time] = time
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    /**
     * @return (attempts, solved, time) or null if no entry exists
     */
    fun getPuzzleStatsForUser(userId: Long, puzzleId: Int): Triple<Int, Boolean, Int>?
    {
        return try
        {
            transaction(puzzleDB) {
                PuzzleStatsByUser.select {
                    (PuzzleStatsByUser.userId eq userId) and (PuzzleStatsByUser.puzzleId eq puzzleId)
                }.firstOrNull()?.let {
                    Triple(it[PuzzleStatsByUser.attempts], it[PuzzleStatsByUser.solved], it[PuzzleStatsByUser.time])
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            null
        }
    }

    fun getPuzzle(puzzleId: Int): Puzzle?
    {
        try
        {
            return transaction(puzzleDB) {
                Puzzles.select {
                    Puzzles.puzzleId eq puzzleId
                }.firstOrNull()?.let {
                    Puzzle.createPuzzle(
                        Util.PuzzleType.fromId(it[Puzzles.puzzleType]),
                        it[Puzzles.puzzleId],
                        it[Puzzles.creatorId],
                        it[Puzzles.puzzleDetails],
                        it[Puzzles.name],
                        it[Puzzles.description],
                        it[Puzzles.difficulty],
                        it[Puzzles.maxAttempts],
                        Util.PuzzleRewardType.fromId(it[Puzzles.rewardType]),
                        it[Puzzles.rewardAmount],
                        it[Puzzles.puzzleTips]?.split(",")?.map { hint -> hint.trim() } ?: emptyList()
                    )
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Gets a random puzzle that is not created by the specified user.
     * @param userId the id of the user that should not be the creator of the puzzle
     * @return a random puzzle that is not created by the specified user
     */
    fun getRandomPuzzle(userId: Long): Puzzle?
    {
        try
        {
            return transaction(puzzleDB) {
                Puzzles.leftJoin(PuzzleStatsByUser, { puzzleId }, { puzzleId }).select {
                    (Puzzles.approved eq true) and (Puzzles.creatorId neq userId) and (PuzzleStatsByUser.solved eq false or PuzzleStatsByUser.solved.isNull())
                }.orderBy(Random()).firstOrNull()?.let {
                    Puzzle.createPuzzle(
                        Util.PuzzleType.fromId(it[Puzzles.puzzleType]),
                        it[Puzzles.puzzleId],
                        it[Puzzles.creatorId],
                        it[Puzzles.puzzleDetails],
                        it[Puzzles.name],
                        it[Puzzles.description],
                        it[Puzzles.difficulty],
                        it[Puzzles.maxAttempts],
                        Util.PuzzleRewardType.fromId(it[Puzzles.rewardType]),
                        it[Puzzles.rewardAmount],
                        it[Puzzles.puzzleTips]?.split(",")?.map { hint -> hint.trim() } ?: emptyList()
                    )
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            return null
        }
    }

    fun setActivePuzzleIdForUser(userId: Long, puzzleId: Int?): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                UserStatsOverall.update({ UserStatsOverall.userId eq userId }) {
                    it[activePuzzle] = puzzleId
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    fun getActivePuzzleIdForUser(userId: Long): Int?
    {
        return try
        {
            transaction(puzzleDB) {
                UserStatsOverall.select {
                    UserStatsOverall.userId eq userId
                }.firstOrNull()?.let {
                    it[UserStatsOverall.activePuzzle]
                }
            }
        } catch (e: ExposedSQLException)
        {
            e.printStackTrace()
            null
        }
    }

    fun initializeUserStatsIfNotExists(userId: Long): DBResponse
    {
        try
        {
            return transaction(puzzleDB) {
                UserStatsOverall.insertIgnore {
                    it[this.userId] = userId
                    it[this.activePuzzle] = null
                    it[this.solvedAmount] = 0
                    it[this.rewardPoints] = 0
                }
                return@transaction DBResponse.SUCCESS
            }
        } catch (e: ExposedSQLException)
        {
            return DBResponse.FAILURE
        }
    }

    //endregion
}