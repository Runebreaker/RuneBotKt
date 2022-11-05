package de.runebot.behaviors

import dev.kord.core.event.message.MessageCreateEvent

object TestBehavior : Behavior
{
    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if (content.contains("test", ignoreCase = true))
        {
            messageCreateEvent.message.channel.createMessage("EKO FRESH?")
        }
    }
}