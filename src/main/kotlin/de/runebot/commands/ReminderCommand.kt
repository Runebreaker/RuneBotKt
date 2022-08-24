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

object ReminderCommand : MessageCommand
{
    override val names: List<String>
        get() = listOf("reminder", "rem")
    private val regex = Regex("\\d+[smhd]")

    override fun prepare(kord: Kord)
    {
        println("Reminder command ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event, "Please specify a time.")
        }
        val rawTime = regex.findAll(args[1], 0).map { result -> result.value }.toList()
        val time = convertTimeToMillis(rawTime)
        DB.addTimer(time, args.subList(2, args.lastIndex + 1).joinToString(" "), event.message.channelId.value.toLong(), event.message.id.value.toLong())
        Taskmaster.updateTimers()
        event.message.addReaction(ReactionEmoji.Unicode(Emojis.alarmClock.unicode))
        Util.sendMessage(event, "Timer was set for ${rawTime.joinToString("")}. React, if you want to get pinged too as soon as the timer runs out!")
    }

    private fun convertTimeToMillis(timePieces: List<String>): Duration
    {
        var totalDuration: Duration = Duration.ZERO
        for (piece in timePieces)
        {
            when (piece.last())
            {
                's' ->
                {
                    totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().seconds)
                }
                'm' ->
                {
                    totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().minutes)
                }
                'h' ->
                {
                    totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().hours)
                }
                'd' ->
                {
                    totalDuration = totalDuration.plus(piece.substring(0, piece.lastIndex).toInt().days)
                }
            }
        }
        return totalDuration
    }
}