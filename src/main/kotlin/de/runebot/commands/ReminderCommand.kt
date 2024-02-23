package de.runebot.commands

import de.runebot.Taskmaster
import de.runebot.Util
import de.runebot.database.DB
import dev.kord.common.toMessageFormat
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.*
import kotlinx.datetime.Clock
import kotlin.time.Duration

object ReminderCommand : RuneTextCommand, RuneSlashCommand
{
    private const val NO_MESSAGE_SET = "You didn't set a message, but your timer expired!"

    private val subscribeSubCommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("subscribe"), "subscribe <id>" to "subscribe to an active reminder"),
        { event, args, _ ->
            val id = args[0].toIntOrNull()
            val userId = event.message.author?.id?.value

            if (id != null && userId != null)
            {
                val timer = DB.subscribeToTimer(id, userId)
                if (timer != null)
                {
                    Util.sendMessage(event, "Successfully subscribed to $timer.")
                }
            }




            Util.sendMessage(event, "${args[0]} is not a valid id. Use `>reminder list <someUser>` to find the right id.") // TODO: create subcommand
        }, emptyList()
    )

    private val reminder = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(names, Pair("reminder <time> <message>", "After specified time, posts the given message and mentions the author and reactors.")),
        { event, args, _ ->
            // Extract time
            val duration = convertInputStringToDuration(args[0])
            val targetTime = Clock.System.now() + duration

            // DB Operation
            val message = if (args.size > 1) args.subList(1, args.size).joinToString(" ") else NO_MESSAGE_SET

            val creatorId = event.message.author?.id?.value

            if (creatorId != null)
            {
                DB.addTimer(creatorId, targetTime, message)?.let { timer ->
                    Taskmaster.updateTimers()
                    Util.sendMessage(
                        event,
                        "Timer was set for ${targetTime.toMessageFormat()}. Subscribe to this reminder with `>reminder subscribe ${timer.id}`."
                    )
                    return@Subcommand
                }
            }
            Util.sendMessage(event, "error creating reminder")
        },
        listOf(subscribeSubCommand)
    )

    override val names: List<String>
        get() = listOf("reminder", "rem")
    override val shortHelpText: String
        get() = "reminds users for the specified reasons"
    override val longHelpText: String
        get() = reminder.toTree().toString()

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
            subCommand("new", "create a new reminder")
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
                integer("id", "id according to /reminder list")
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
                    val targetTime = Clock.System.now() + duration
                    val creatorId = interaction.user.id.value

                    DB.addTimer(creatorId, targetTime, message)?.let { timer ->
                        Taskmaster.updateTimers()
                        interaction.respondPublic {
                            content = "Timer was set for ${targetTime.toMessageFormat()}. Subscribe to this reminder with `/reminder subscribe ${timer.id}`."
                        }
                        return
                    }

                    interaction.respondEphemeral { content = "error creating reminder" }
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