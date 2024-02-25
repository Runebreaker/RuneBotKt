package de.runebot.commands

import de.runebot.Taskmaster
import de.runebot.Util
import de.runebot.database.DB
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.*
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlin.time.Duration

object ReminderCommand : RuneTextCommand, RuneSlashCommand
{
    private const val NO_MESSAGE_SET = "You didn't set a message, but your timer expired!"

    private val subscribeSubcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("subscribe", "sub"), "subscribe <id>" to "Subscribe to an active reminder."),
        { event, args, _ ->
            val id = args[0].toIntOrNull()
            val userId = event.message.author?.id?.value

            if (id != null && userId != null)
            {
                val timer = DB.subscribeToTimer(id, userId)
                if (timer != null)
                {
                    Taskmaster.updateTimers()
                    Util.sendMessage(event, "Successfully subscribed to $timer.")
                    return@Subcommand
                }
            }

            Util.sendMessage(event, "${args[0]} is not a valid id. Use `>reminder list <someUser>` to find the right id.")
        }, emptyList()
    )

    private val listSubcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("list"), "list [<user>]" to "List a user's active reminders."),
        { event, _, _ ->
            val mentions = event.message.mentionedUsers.toList()
            val creator = mentions.firstOrNull() ?: event.message.author

            if (creator == null)
            {
                Util.sendMessage(event, "Error finding user!")
                return@Subcommand
            }

            val timers = DB.getTimersForCreator(creator.id.value)
                .sortedBy { it.targetTime }

            if (timers.isEmpty())
            {
                Util.sendMessage(event, "No active reminders found for ${creator.effectiveName}.")
                return@Subcommand
            }

            Util.sendMessage(event, timers.joinToString(separator = "\n", prefix = "**Active reminders for ${creator.effectiveName}:**\n")
            {
                "- $it"
            })
            return@Subcommand
        }, emptyList()
    )

    private val reminder = RuneTextCommand.Subcommand( // TODO: remove reminder if owner
        RuneTextCommand.CommandDescription(names, Pair("reminder <time> [<message>]", "After the specified time, you and all subscribers will be dm-ed with a custom message.")),
        { event, args, _ ->
            // Extract time
            val duration = convertInputStringToDuration(args[0])

            if (duration == null)
            {
                Util.sendMessage(event, "Invalid time formatting: \"${args[0]}\"")
                return@Subcommand
            }

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
                        "Successfully created $timer.\nSubscribe to this reminder with `>reminder subscribe ${timer.id}`."
                    )
                    return@Subcommand
                }
            }
            Util.sendMessage(event, "Error creating reminder.")
        },
        listOf(subscribeSubcommand, listSubcommand)
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

    private fun convertInputStringToDuration(input: String): Duration?
    {
        return Duration.parseOrNull(input)
    }

    override val name: String
        get() = "reminder"
    override val helpText: String
        get() = "create reminders"

    override suspend fun createCommand(builder: GlobalChatInputCreateBuilder) // TODO: remove reminder if owner
    {
        with(builder)
        {
            subCommand("new", "Create a new reminder.")
            {
                string("duration", "Duration after now until the reminder will trigger.")
                {
                    required = true
                }
                string("message", "What to display when the reminder triggers.")
                {
                    required = false
                }
            }
            subCommand("list", "List a user's currently active reminders.")
            {
                user("target", "What user's reminders to list.")
                {
                    required = false
                }
            }
            subCommand("subscribe", "Subscribe to an active reminder.")
            {
                integer("id", "Id according to /reminder list.")
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
                "new" ->
                {
                    val durationString = interaction.command.strings["duration"]!!
                    val duration = convertInputStringToDuration(durationString)

                    if (duration == null)
                    {
                        interaction.respondEphemeral {
                            content = "Invalid time formatting: \"$durationString\""
                        }
                        return
                    }

                    val message = interaction.command.strings["message"] ?: NO_MESSAGE_SET
                    val targetTime = Clock.System.now() + duration
                    val creatorId = interaction.user.id.value

                    DB.addTimer(creatorId, targetTime, message)?.let { timer ->
                        Taskmaster.updateTimers()
                        interaction.respondPublic {
                            content = "Successfully created $timer.\nSubscribe to this reminder with `/reminder subscribe ${timer.id}`."
                        }
                        return
                    }

                    interaction.respondEphemeral { content = "error creating reminder" }
                }

                "list" ->
                {
                    val creator = interaction.command.users["target"] ?: interaction.user
                    val timers = DB.getTimersForCreator(creator.id.value)
                        .sortedBy { it.targetTime }

                    if (timers.isEmpty())
                    {
                        interaction.respondEphemeral {
                            content = "No active reminders found for ${creator.effectiveName}."
                        }
                        return
                    }

                    interaction.respondEphemeral {
                        content = timers.joinToString(separator = "\n", prefix = "**Active reminders for ${creator.effectiveName}:**\n")
                        {
                            "- $it"
                        }
                    }
                    return
                }

                "subscribe" ->
                {
                    val id = interaction.command.integers["id"]!!
                    val userId = interaction.user.id.value

                    val timer = DB.subscribeToTimer(id.toInt(), userId)
                    if (timer != null)
                    {
                        Taskmaster.updateTimers()
                        interaction.respondEphemeral {
                            content = "Successfully subscribed to $timer."
                        }
                        return
                    }

                    interaction.respondEphemeral {
                        content = "$id is not a valid id. Use `/reminder list` to find the right id."
                    }
                }

                else -> interaction.respondEphemeral { content = "Error." }
            }
        }
    }
}