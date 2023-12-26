package de.runebot.behaviors

import de.runebot.Util
import de.runebot.Util.guildEmojiRegex
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.text.RegexOption.DOT_MATCHES_ALL

object SixtyNine : Behavior
{
    val anythingOrGuildEmojiRegex = Regex("(.*$guildEmojiRegex.*|.*)")
    val star
        get() = anythingOrGuildEmojiRegex

    val regexes = listOf(
        Regex(
            "([sS])(.*)([iI])(.*)([xX])(.*)([tT])(.*)([yY])(.*)([nN])(.*)([iI])(.*)([nN])(.*)([eE])|()([sS])(.*)([iI])(.*)([xX])(.*)([nN])(.*)([iI])(.*)([nN])(.*)([eE])", // empty group is necessary for grouping indices
            DOT_MATCHES_ALL
        ),
        Regex("(L)(.*)(X)(.*)(I)(.*)(X)", DOT_MATCHES_ALL),
        Regex("([nN])(.*)([eE])(.*)([uU])(.*)([nN])(.*)([uU])(.*)([nN])(.*)([dD])(.*)([sS])(.*)([eE])(.*)([cC])(.*)([hH])(.*)([zZ])(.*)([iI])(.*)([gG])", DOT_MATCHES_ALL),
        Regex("([sS])(.*)([eE])(.*)([cC])(.*)([hH])(.*)([sS])(.*)([nN])(.*)([eE])(.*)([uU])(.*)([nN])", DOT_MATCHES_ALL)
    )

    val regexes2 = listOf(
        Regex(
            "$star([sS])$star([iI])$star([xX])$star([tT])$star([yY])$star([nN])$star([iI])$star([nN])$star([eE])$star|()$star([sS])$star([iI])$star([xX])$star([nN])$star([iI])$star([nN])$star([eE])$star", // empty group is necessary for grouping indices
            DOT_MATCHES_ALL
        ),
        Regex("$star(L)$star(X)$star(I)$star(X)$star", DOT_MATCHES_ALL),
        Regex(
            "$star([nN])$star([eE])$star([uU])$star([nN])$star([uU])$star([nN])$star([dD])$star([sS])$star([eE])$star([cC])$star([hH])$star([zZ])$star([iI])$star([gG])$star",
            DOT_MATCHES_ALL
        ),
        Regex("$star([sS])$star([eE])$star([cC])$star([hH])$star([sS])$star([nN])$star([eE])$star([uU])$star([nN])$star", DOT_MATCHES_ALL)
    )

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        regexes.forEach { regex ->

            var i = 0
            val rangesToReplace = guildEmojiRegex.findAll(content).map {
                val intRange = i..<it.range.first
                i = it.range.last + 1
                intRange
            }.toMutableList()
            rangesToReplace.add(i..<content.length)

            rangesToReplace.forEach { range ->
            }

            val withHighlight = content
                .replace(guildEmojiRegex) {
                    it.value
                }
//                .replace(regex) { result ->
////                result.groups.mapIndexed { index, group -> index to group }
////                    .joinToString(separator = "") { (index, group) ->
////                        if (group == null || index == 0) ""// index 0 is whole match, this is to be ignored
////                        else if (index % 2 == 1 || group.value.isBlank()) group.value // groups are 1-indexed, so every odd group is to be highlighted
////                        else "[${group.value}](http://./)"
////                    }

            if (withHighlight != content)
            {
                Util.sendMessage(messageCreateEvent, "Nice: \"$withHighlight\"")
            }
        }
    }
}