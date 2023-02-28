package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent

object Doggo : Behavior
{
    val doggoGif = "https://media.discordapp.net/attachments/824357369559121920/914912078626693130/656954222046347274.gif"

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        if (doggoGif in content)
        {
            Util.sendMessage(messageCreateEvent, doggoGif)
        }
    }
}