package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent

object Lenny : Behavior
{
    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if ("lenny" in content.lowercase())
        {
            Util.sendMessage(messageCreateEvent, "( ͡° ͜ʖ ͡°)")
        }
    }
}