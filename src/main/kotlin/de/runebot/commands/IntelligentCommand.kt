package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object IntelligentCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("intelligent")
    override val shortHelpText: String
        get() = "sends dank meme gif"
    override val longHelpText: String
        get() = "`$commandExample`: sends gif"

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        Util.sendMessage(event, "https://tenor.com/view/buzz-lightyear-no-sign-of-intelligent-life-dumb-toy-story-gif-11489315")
    }
}