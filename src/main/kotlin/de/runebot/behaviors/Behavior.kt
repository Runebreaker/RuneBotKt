package de.runebot.behaviors

import de.runebot.config.Config
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

    fun isEnabled(guildId: ULong, channelId: ULong): Boolean
    {
        this::class.simpleName?.let { className ->
            return Config.getEnabledBehaviour(guildId, channelId, className)
        } ?: return false
    }

    suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
}