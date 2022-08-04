package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.x.emoji.Emojis

object FBehavior : Behavior
{
    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if (content.equals("F", ignoreCase = true) || content == Emojis.regionalIndicatorF.unicode)
        {
            Util.sendMessage(messageCreateEvent, content)
        }
    }
}