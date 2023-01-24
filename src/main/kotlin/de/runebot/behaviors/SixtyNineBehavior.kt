package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

object SixtyNineBehavior : Behavior
{
    val regexes = listOf(
        Regex("69"),
        Regex("s.*i.*x.*t.*y.*n.*i.*n.*e", setOf(DOT_MATCHES_ALL, IGNORE_CASE)),
        Regex("n.*e.*u.*n.*u.*n.*d.*s.*e.*c.*h.*z.*i.*g", setOf(DOT_MATCHES_ALL, IGNORE_CASE)),
        Regex("s.*e.*c.*h.*s.*n.*e.*u.*n", setOf(DOT_MATCHES_ALL, IGNORE_CASE)),
    )

    fun replace69(regex: Regex, matchResult: MatchResult): String
    {
        var replacement = ""
        var left = 0
        var right = 0

        regex.pattern.filter { it.isLetterOrDigit() }.forEach { c ->
            val indexOf = matchResult.value.substring(left).indexOf(c, ignoreCase = true)
            if (indexOf == -1) return@forEach
            right = indexOf + 1 + left
            replacement += matchResult.value.substring(left, right - 1) + "__" + c + "__"
            left = right
        }
        replacement += matchResult.value.substring(left)

        return replacement.replace("____", "")
    }

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        var sixtynine = false
        regexes.forEach { regex ->
            if (!regex.containsMatchIn(content)) return@forEach
            if (regex.pattern == "s.*i.*x.*t.*y.*n.*i.*n.*e") sixtynine = true

            val sendThis = regex.replace(content) { matchResult ->
                replace69(regex, matchResult)
            }

            Util.sendMessage(messageCreateEvent, "Nice: \"$sendThis\"")
        }

        if (!sixtynine)
        {
            Regex("s.*i.*x.*n.*i.*n.*e", setOf(DOT_MATCHES_ALL, IGNORE_CASE)).let { regex ->
                if (!regex.containsMatchIn(content)) return@let
                Util.sendMessage(messageCreateEvent, "Nice: \"${regex.replace(content) { matchResult -> replace69(regex, matchResult) }}\"")
            }
        }
    }
}