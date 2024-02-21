package de.runebot.commands

import de.runebot.Registry
import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object HelpCommand : RuneTextCommand
{
    override val names: List<String>
        get() = listOf("help", "?")
    override val shortHelpText: String
        get() = "get help for commands"
    override val longHelpText: String
        get() = "`$commandExample`: show help overview\n" +
                "`$commandExample command`: show detailed help for given command"

    override suspend fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        // show overview
        if (args.size < 2)
        {
            Util.sendMessage(
                event, Registry.commands
                    .filterIsInstance<RuneTextCommand>()
                    .filter { !it.needsAdmin || RuneTextCommand.isAdmin(event) }
                .sortedBy { it.names.first() }
                .joinToString(prefix = "Available commands:${System.lineSeparator()}", separator = System.lineSeparator()) { cmd ->
                    cmd.names.joinToString(prefix = "`", separator = "`|`", postfix = "`: ") { RuneTextCommand.prefix + it } + cmd.shortHelpText
                })
        }
        // show specific help text
        else
        {
            Util.sendMessage(event, Registry.commands.filterIsInstance<RuneTextCommand>().find { args.getOrNull(1) in it.names }?.longHelpText ?: "command not found!")
        }
    }
}