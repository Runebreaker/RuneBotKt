package de.runebot.commands

import de.runebot.Util
import de.runebot.behaviors.Frequency
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object FrequencyCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("frequency", "freq")
    override val shortHelpText: String
        get() = "" // TODO
    override val longHelpText: String
        get() = "" // TODO

    override fun prepare(kord: Kord)
    {
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val author = event.message.author?.id ?: return
        Util.sendMessage(event, "You have ${Frequency.getScoreFor(author)}")
    }
}