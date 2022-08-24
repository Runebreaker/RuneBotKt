package de.runebot.commands

import de.runebot.Registry
import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object HelpCommand : MessageCommand
{
    override val names: List<String>
        get() = listOf("help", "?")
    override val shortHelpText: String
        get() = "get help for commands"
    override val longHelpText: String
        get() = "`$commandExample`: show help overview\n" +
                "`$commandExample command`: show detailed help for given command"

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val sb = StringBuilder()

        // show overview
        if (args.size < 2)
        {
            sb.append("Available commands:")
            Registry.messageCommands
                .filter { MessageCommand.isAdmin(event) || !it.needsAdmin }
                .forEach { cmd ->
                    sb.append("\n")
                    sb.append(cmd.names.joinToString(prefix = "`", separator = "`|`", postfix = "`: ") { MessageCommand.prefix + it })
                    sb.append(cmd.shortHelpText)
                }
        }
        else
        {
            sb.append(Registry.messageCommands.find { args.getOrNull(1) in it.names }?.longHelpText ?: return)
        }

        Util.sendMessage(event, sb.toString())
    }
}