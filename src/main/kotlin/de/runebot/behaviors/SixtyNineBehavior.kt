package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

object SixtyNineBehavior : Behavior
{
    val regexes = listOf(
        Regex("(s|S).*(i|I).*(x|X).*(t|T).*(y|Y).*(n|N).*(i|I).*(n|N).*(e|E)", DOT_MATCHES_ALL) to "sixtynine",
        Regex("69") to "69",
        Regex("6️⃣9️⃣") to "6️⃣9️⃣",
        Regex("LXIX") to "LXIX",
        Regex("(n|N).*(e|E).*(u|U).*(n|N).*(u|U).*(n|N).*(d|D).*(s|S).*(e|E).*(c|C).*(h|H).*(z|Z).*(i|I).*(g|G)", DOT_MATCHES_ALL) to "neunundsechzig",
        Regex("(s|S).*(e|E).*(c|C).*(h|H).*(s|S).*(n|N).*(e|E).*(u|U).*(n|N)", DOT_MATCHES_ALL) to "sechsneun",
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
            Regex("(s|S).*(i|I).*(x|X).*(n|N).*(i|I).*(n|N).*(e|E)", setOf(DOT_MATCHES_ALL, IGNORE_CASE)).let { regex ->
                if (!regex.containsMatchIn(content)) return@let
                Util.sendMessage(messageCreateEvent, "Nice: \"${regex.replace(content) { matchResult -> replace69("sixnine", matchResult) }.replace("____", "")}\"")
            }
        }
    }
}