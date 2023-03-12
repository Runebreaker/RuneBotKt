package de.runebot.behaviors

import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.x.emoji.Emojis

object Listen : Behavior
{
    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if (content.equals("listen", ignoreCase = true))
        {
            messageCreateEvent.message.addReaction(ReactionEmoji.Unicode(Emojis.ear.unicode))
        }
    }
}