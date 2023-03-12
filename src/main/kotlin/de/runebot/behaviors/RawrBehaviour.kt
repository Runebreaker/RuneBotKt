package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent

object RawrBehaviour : Behavior
{
    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if ("rawr" in content.lowercase()) Util.sendMessage(messageCreateEvent, "It means \"I love you :heart:\" in dinosaur. :t_rex:")
    }
}