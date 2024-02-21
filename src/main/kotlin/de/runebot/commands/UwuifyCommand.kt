package de.runebot.commands

import de.runebot.Util
import de.runebot.Util.replaceUsingRuleset
import de.runebot.config.Config
import dev.kord.common.entity.MessageType
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.GlobalMessageCommandCreateBuilder
import kotlin.math.tanh

object UwuifyCommand : RuneTextCommand, RuneMessageCommand
{
    private val uwuify = RuneTextCommand.Subcommand(
        commandDescription = RuneTextCommand.CommandDescription(names, Pair("uwu ( <input> | reply )", shortHelpText)),
        function = { event, args, _ ->
            if (event.message.type == MessageType.Reply)
            {
                event.message.referencedMessage?.let {
                    Util.sendMessage(event, translate(it.content, event.guildId?.value))
                } ?: Util.sendMessage(event, "Message is not available.")
            }
            else if (args.isNotEmpty())
            {
                Util.sendMessage(event, translate(args.joinToString(" "), event.guildId?.value))
            }
            else Util.sendMessage(event, "Please reply to a message or give text as input.")
        },
        subcommands = emptyList()
    )

    override val names: List<String>
        get() = listOf("uwu", "twanswate", "uwuify", "translate", "furry")
    override val shortHelpText: String
        get() = "makes text kawaii"
    override val longHelpText: String
        get() = uwuify.toTree().toString()

    private const val UWU_PERCENT_BARRIER_MAX = 0.3

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        uwuify.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun translate(input: String, guildId: ULong?): String // Mainly for rules implementation
    {
        val ruleset = if (guildId != null) Config.getRules(guildId).toSet() else emptySet()

        // result, replace count
        val resultPair: Pair<String, Int> = replaceUsingRuleset(input, ruleset)
        var result: String = resultPair.first

        // tangent hyperbolicus curve to determine the percentage of uwu appendage
        val percent: Double = (-UWU_PERCENT_BARRIER_MAX / 2.0) * tanh(input.length / 5.0 - 6.0) + UWU_PERCENT_BARRIER_MAX / 2.0
        if (resultPair.second.toDouble() / input.length.toDouble() <= percent)
            result = "$result uwu"

        return result
    }

    override val name: String
        get() = "uwuify"

    override suspend fun createCommand(builder: GlobalMessageCommandCreateBuilder)
    {
        // nothing to declare
    }

    override suspend fun execute(event: MessageCommandInteractionCreateEvent)
    {
        with(event)
        {
            interaction.respondPublic {
                content = interaction.target.asMessageOrNull()?.content?.let { translate(it, interaction.target.asMessageOrNull()?.getGuildOrNull()?.id?.value) }
            }
        }
    }
}