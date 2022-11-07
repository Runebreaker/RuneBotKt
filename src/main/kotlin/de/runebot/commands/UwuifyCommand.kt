package de.runebot.commands

import de.runebot.Util
import de.runebot.config.Config
import dev.kord.common.entity.MessageType
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.math.tanh

object UwuifyCommand : MessageCommandInterface
{
    private val uwuify = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(names, Pair("uwu ( <input> | reply )", shortHelpText)),
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

    private val wuleset = mutableSetOf<Rule>()
    private const val UWU_PERCENT_BARRIER_MAX = 0.3

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        wuleset.clear()
        wuleset.addAll(Config.getRules())

        uwuify.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun translate(input: String): String // Mainly for rules implementation
    {
        var result = input
        var uwuCounter = 0

        wuleset.forEach { wule ->
            val ruleRegex = Regex(wule.regex)
            ruleRegex.find(input)?.let { uwuCounter++ }
            result = ruleRegex.replace(result, wule.replace)
        }

        // tangent hyperbolicus curve to determine the percentage of uwu appendage
        val percent = (-UWU_PERCENT_BARRIER_MAX / 2.0) * tanh(input.length / 5.0 - 6.0) + UWU_PERCENT_BARRIER_MAX / 2.0
        if (uwuCounter.toDouble() / input.length.toDouble() <= percent)
            result = "$result uwu"

        return result
    }

    data class Rule(val regex: String = "", val replace: String = "")
}