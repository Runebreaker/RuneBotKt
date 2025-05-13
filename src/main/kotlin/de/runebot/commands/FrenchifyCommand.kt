package de.runebot.commands

import de.runebot.Util
import dev.kord.common.entity.MessageType
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.GlobalMessageCommandCreateBuilder
import dev.kord.rest.builder.interaction.GlobalMessageCommandModifyBuilder

object FrenchifyCommand : RuneTextCommand, RuneMessageCommand
{
    private val frenchify = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(names, Pair("frenchify ( <input> | reply )", shortHelpText)),
        { event, args, _ ->
            if (event.message.type == MessageType.Reply)
            {
                event.message.referencedMessage?.let {
                    Util.sendMessage(event, translate(it.content))
                } ?: Util.sendMessage(event, "Message is not available.")
            }
            else if (args.isNotEmpty())
            {
                Util.sendMessage(event, translate(args.joinToString(" ")))
            }
            else Util.sendMessage(event, "Please reply to a message or give text as input.")
        },
        emptyList()
    )

    override val names: List<String>
        get() = listOf("frenchify", "french")
    override val shortHelpText: String
        get() = "translates given text to french correctly"
    override val longHelpText: String
        get() = frenchify.toTree().toString()

    private val rules = mutableMapOf(
        "a" to "ée",
        "b" to "bou",
        "c" to "cie",
        "d" to "Deau",
        "e" to "Euaoix",
        "f" to "feu",
        "g" to "Garçon",
        "h" to "VLE CON D'TA GRAND-MERE",
        "i" to "làèìòù",
        "j" to ",j",
        "k" to "Kours",
        "l" to "\uD83C\uDD7B",
        "m" to "Meu (comme une vache xptdrrr)",
        "n" to "\uD83D\uDC9B**soleil**\uD83D\uDC9B",
        "o" to "974",
        "p" to "pet",
        "q" to "boule d'energie",
        "r" to "r",
        "s" to "Jean-pierre",
        "t" to "__Ouireuxleaussielledeuroyeauou__",
        "u" to "Ue",
        "v" to "Vursilitée",
        "w" to "**2€**",
        "x" to "emmanuel macron",
        "y" to "i grec",
        "z" to "pédale"
    )

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        frenchify.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun translate(input: String): String
    {
        val bobTheBuilder: StringBuilder = StringBuilder()

        input.lowercase().forEach { char ->
            rules[char.toString()]?.let {
                bobTheBuilder.append(it)
            } ?: bobTheBuilder.append(char.toString())
        }

        // Check for markdown edge cases

        val returnString = bobTheBuilder.toString()
        return Regex("\\*\\*\\*\\*").replace(returnString, "")
    }

    override val name: String
        get() = "frenchify"

    override fun createCommand(builder: GlobalMessageCommandCreateBuilder)
    {
        // nothing to declare
    }

    override fun editCommand(builder: GlobalMessageCommandModifyBuilder)
    {
        // nothing to declare
    }

    override suspend fun execute(event: MessageCommandInteractionCreateEvent)
    {
        with(event)
        {
            interaction.respondPublic { content = interaction.target.asMessageOrNull()?.content?.let { translate(it) } }
        }
    }
}