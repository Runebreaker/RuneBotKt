package de.runebot.behaviors

import de.runebot.config.Config
import dev.kord.core.event.message.MessageCreateEvent

interface Behavior
{
    fun isEnabled(guildId: ULong, channelId: ULong): Boolean
    {
        this::class.simpleName?.let { className ->
            return Config.isBehaviorEnabled(guildId, channelId, className)
        } ?: return false
    }

    suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
}