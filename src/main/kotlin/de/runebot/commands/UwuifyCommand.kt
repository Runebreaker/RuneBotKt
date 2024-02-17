package de.runebot.commands

import de.runebot.Util
import de.runebot.Util.Rule
import de.runebot.Util.replaceUsingRuleset
import de.runebot.config.Config
import dev.kord.common.entity.MessageType
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.math.tanh

object UwuifyCommand : RuneMessageCommand
{
    private val uwuify = RuneMessageCommand.Subcommand(
        RuneMessageCommand.CommandDescription(names, Pair("uwu ( <input> | reply )", shortHelpText)),
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
        get() = listOf("uwu", "twanswate", "uwuify", "translate", "furry")
    override val shortHelpText: String
        get() = "makes text kawaii"
    override val longHelpText: String
        get() = uwuify.toTree().toString()

    private val ruleset = mutableSetOf<Rule>()
    private const val UWU_PERCENT_BARRIER_MAX = 0.3

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        ruleset.clear()
        ruleset.addAll(Config.getRules(event.guildId?.value ?: return))

        uwuify.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun translate(input: String): String // Mainly for rules implementation
    {
        // result, replace count
        val resultPair: Pair<String, Int> = replaceUsingRuleset(input, ruleset)
        var result: String = resultPair.first

        // tangent hyperbolicus curve to determine the percentage of uwu appendage
        val percent: Double = (-UWU_PERCENT_BARRIER_MAX / 2.0) * tanh(input.length / 5.0 - 6.0) + UWU_PERCENT_BARRIER_MAX / 2.0
        if (resultPair.second.toDouble() / input.length.toDouble() <= percent)
            result = "$result uwu"

        return result
    }
}