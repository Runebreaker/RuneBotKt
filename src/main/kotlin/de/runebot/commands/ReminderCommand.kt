package de.runebot.commands

import de.runebot.Taskmaster
import de.runebot.Util
import de.runebot.database.DB
import de.runebot.database.DBResponse.SUCCESS
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.*
import dev.kord.x.emoji.Emojis
import kotlin.time.Duration

object ReminderCommand : RuneTextCommand, RuneSlashCommand
{
    private const val NO_MESSAGE_SET = "You didn't set a message, but your timer expired!"

    private val reminder = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(names, Pair("reminder <time> <message>", "After specified time, posts the given message and mentions the author and reactors.")),
        { event, args, _ ->
            // Extract time
            val duration = convertInputStringToDuration(args[0])

            // DB Operation
            val userMessage = if (args.size > 1) args.subList(1, args.size).joinToString(" ") else NO_MESSAGE_SET

            DB.addTimer(duration, userMessage, event.message.channelId.value.toLong(), event.message.id.value.toLong())

            // Taskmaster foo
            Taskmaster.updateTimers()

            // Response message foo
            event.message.addReaction(ReactionEmoji.Unicode(Emojis.alarmClock.unicode))
            Util.sendMessage(event, "Timer was set for ${duration}. React, if you want to get pinged too as soon as the timer runs out!")
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

    private fun convertInputStringToDuration(input: String): Duration
    {
        return Duration.parseOrNull(input) ?: Duration.ZERO
    }

    override val name: String
        get() = "reminder"
    override val helpText: String
        get() = "create reminders"

    override suspend fun createCommand(builder: GlobalChatInputCreateBuilder)
    {
        with(builder)
        {
            subCommand("create", "create a new reminder")
            {
                string("duration", "duration after now until the reminder will trigger")
                {
                    required = true
                }
                string("message", "what to display when reminder triggers")
                {
                    required = false
                }
            }
            subCommand("list", "list a users currently active reminders")
            {
                user("target", "what user's reminders to list")
                {
                    required = false
                }
            }
            subCommand("subscribe", "subscribe to an active reminder")
            {
                user("target", "who this reminder belongs to")
                {
                    required = true
                }
                integer("index", "which one according to /reminder list")
                {
                    required = true
                }
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val subcommands = interaction.command.data.options.value?.map { it.name } ?: emptyList()

            when (subcommands.firstOrNull())
            {
                "create" ->
                {
                    val durationString = interaction.command.strings["duration"]!!
                    val duration = convertInputStringToDuration(durationString)
                    val message = interaction.command.strings["message"] ?: NO_MESSAGE_SET
                    val targetTime = System.currentTimeMillis() + duration.inWholeMilliseconds
                    val channelId = interaction.channelId.value.toLong()

                    val dbResponse = DB.addTimer(targetTime, message, channelId, -1) // -1 should not be an occurring snowflake, as we ignore that part of the DB.

                    when (dbResponse)
                    {
                        SUCCESS ->
                        {
                            Taskmaster.updateTimers()
                            val timerIndex = DB.getAllOldTimers().mapIndexed { index: Int, oldTimerEntry: DB.OldTimerEntry -> index to oldTimerEntry }
                                .find { DB.OldTimerEntry(targetTime, message, channelId, -1) == it.second }?.first

                            interaction.respondPublic {
                                content = "Timer was set for ${duration}." + if (timerIndex != null) " Subscribe to this reminder with `/reminder subscribe $timerIndex`." else ""
                            }
                        }

                        else ->
                        {
                            interaction.respondEphemeral { content = "error creating reminder" }
                        }
                    }


                }

                "list" ->
                {

                }

                "subscribe" ->
                {

                }

                else -> interaction.respondEphemeral { content = "error" }
            }
        }
    }
}