package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent

object StyleBehavior : Behavior
{
    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if (content.contains("style", ignoreCase = true))
        {
            Util.sendMessage(messageCreateEvent, "und das Geld.")
        }
    }
}