package de.runebot.commands

import dev.kord.core.Kord
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.toReaction
import redempt.crunch.Crunch

object EvalCommand : RuneTextCommand
{
    override val names: List<String>
        get() = listOf("eval")
    override val shortHelpText: String
        get() = "evaluate simple math expressions"
    override val longHelpText: String
        get() = "$commandExample `input`: evaluate the input math expression"

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        let {
            val expression = args.drop(1).joinToString(" ")
            if (expression.isBlank()) return@let

            try
            {
                val result = Crunch.compileExpression(expression).evaluate()

                event.message.reply {
                    content = (if (result.toInt().toDouble() == result) result.toInt() else result).toString()
                }
            } catch (_: Exception)
            {
                return@let
            }

            return
        }

        event.message.addReaction(Emojis.x.toReaction())
    }
}