package de.runebot.commands

import de.runebot.Taskmaster
import de.runebot.Util
import de.runebot.database.DB
import dev.kord.core.Kord
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.x.emoji.Emojis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ReminderCommand : RuneMessageCommand
{
    private val reminder = RuneMessageCommand.Subcommand(
        RuneMessageCommand.CommandDescription(names, Pair("reminder <time> <message>", "After specified time, posts the given message and mentions the author and reactors.")),
        { event, args, _ ->
            // Extract time
            val rawTime = regex.findAll(args[0], 0).map { result -> result.value }.toList()
            val time = convertTimeToMillis(rawTime)

            // DB Operation
            val userMessage = if (args.size > 1) args.subList(1, args.size).joinToString(" ") else "You didn't set a message, but your timer expired!"
            DB.addTimer(time, userMessage, event.message.channelId.value.toLong(), event.message.id.value.toLong())

            // Taskmaster foo
            Taskmaster.updateTimers()

            // Response message foo
            event.message.addReaction(ReactionEmoji.Unicode(Emojis.alarmClock.unicode))
            Util.sendMessage(event, "Timer was set for ${rawTime.joinToString("")}. React, if you want to get pinged too as soon as the timer runs out!")
        },
        emptyList()
    )

    override val names: List<String>
        get() = listOf("reminder", "rem")
    override val shortHelpText: String
        get() = "reminds users for the specified reasons"
    override val longHelpText: String
        get() = reminder.toTree().toString()
    private val regex = Regex("\\d+[smhd]")

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event, "Please specify a time.")
        }
        if (names.contains(args[0].substring(1))) reminder.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun convertTimeToMillis(timePieces: List<String>): Duration
    {
        var totalDuration: Duration = Duration.ZERO
        for (piece in timePieces)
        {
            when (piece.last())
            {
                's' -> totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().seconds)
                'm' -> totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().minutes)
                'h' -> totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().hours)
                'd' -> totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().days)
            }
        }
        return totalDuration
    }
}