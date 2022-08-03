package de.runebot.behaviors

import dev.kord.core.event.message.MessageCreateEvent

interface Behavior
{
    companion object
    {
        suspend fun runAll(content: String, messageCreateEvent: MessageCreateEvent)
        {
            TestBehavior.run(content, messageCreateEvent)
        }
    }

    suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
}