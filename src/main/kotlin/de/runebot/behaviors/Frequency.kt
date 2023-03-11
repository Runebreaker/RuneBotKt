package de.runebot.behaviors

import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.Path

object Frequency : Behavior
{
    private val frequency = mutableMapOf<String, Long>()

    private val scores = mutableMapOf<Snowflake, Long>()

    init
    {
        Database.connect("jdbc:sqlite:${Path("freqDB.sqlite")}")

        transaction {
            SchemaUtils.create(Frequencies, Scores)
        }
    }

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        content.split(" ").forEach {
            val word = it.filter { it.isLetterOrDigit() }
            val currentFreq = frequency.getOrPut(word) { 0 }
            val score = frequency.count { it.value > currentFreq }
            val author = messageCreateEvent.message.author?.id ?: return
            scores[author] = scores.getOrPut(author) { 0 } + score
            frequency[word] = currentFreq + 1
        }
        println(frequency)
        println(scores)
    }

    private fun increment(word: String)
    {
        transaction {
            Frequencies.update({ Frequencies.word eq word })
            {
                // TODO
            }
        }
    }

    fun getScoreFor(id: Snowflake) = scores.getOrPut(id) { 0 }
}

object Frequencies : Table()
{
    val word: Column<String> = varchar("word", 4000)
    val count: Column<Long> = long("count")
}

object Scores : Table()
{
    val userId: Column<Long> = long("userId")
    val score: Column<Long> = long("score")
}