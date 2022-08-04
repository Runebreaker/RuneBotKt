package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent

object RedditBehavior : Behavior
{
    private val regex = Regex("(?<=(?:^|\\s))[rR]\\/[\\w\\d]+")

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        val responseSB = StringBuilder()
        regex.findAll(content).forEachIndexed { index, matchResult ->
            if (index != 0) responseSB.append("\n")
            responseSB.append("https://old.reddit.com/${matchResult.value}/")
        }
        if (responseSB.isNotBlank()) Util.sendMessage(messageCreateEvent, responseSB.toString())
    }
}