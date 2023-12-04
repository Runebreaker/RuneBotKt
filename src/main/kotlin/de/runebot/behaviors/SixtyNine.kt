package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

object SixtyNine : Behavior
{
    val regexes = listOf(
        Regex("([sS]).*([iI]).*([xX]).*([tT]).*([yY]).*([nN]).*([iI]).*([nN]).*([eE])", DOT_MATCHES_ALL),
        Regex("(L).*(X).*(I).*(X)", DOT_MATCHES_ALL),
        Regex("([nN]).*([eE]).*([uU]).*([nN]).*([uU]).*([nN]).*([dD]).*([sS]).*([eE]).*([cC]).*([hH]).*([zZ]).*([iI]).*([gG])", DOT_MATCHES_ALL),
        Regex("([sS]).*([eE]).*([cC]).*([hH]).*([sS]).*([nN]).*([eE]).*([uU]).*([nN])", DOT_MATCHES_ALL)
    )

    fun replace69(str: String, matchResult: MatchResult): String
    {
        var replacement = ""
        var left = 0
        var right = 0

        str.forEach { c ->
            val indexOf = matchResult.value.substring(left).indexOf(c, ignoreCase = true)
            if (indexOf == -1) return@forEach
            right = indexOf + 1 + left
            replacement += matchResult.value.substring(left, right - 1) + "__" + matchResult.value[right - 1] + "__"
            left = right
        }
        replacement += matchResult.value.substring(left)

        return replacement
    }

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        regexes.forEach { regex ->
            content.replace(regex) { matchResult ->
                matchResult.groups.first().
            }
        }

        var sixtynine = false
        regexes.forEach { (regex, str) ->
            if (!regex.containsMatchIn(content)) return@forEach
            if (regex == regexes[0].first) sixtynine = true

            val sendThis = regex.replace(content) { matchResult ->
                replace69(str, matchResult)
            }

            Util.sendMessage(messageCreateEvent, "Nice: \"${sendThis.replace("____", "")}\"")
        }

        if (!sixtynine)
        {
            Regex("([sS]).*([iI]).*([xX]).*([nN]).*([iI]).*([nN]).*([eE])", setOf(DOT_MATCHES_ALL, IGNORE_CASE)).let { regex ->
                if (!regex.containsMatchIn(content)) return@let
                Util.sendMessage(messageCreateEvent, "Nice: \"${regex.replace(content) { matchResult -> replace69("sixnine", matchResult) }.replace("____", "")}\"")
            }
        }
    }
}