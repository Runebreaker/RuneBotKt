package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

interface StringResponseCommand : MessageCommandInterface
{
    val response: String

    override suspend fun prepare(kord: Kord) = Unit

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        Util.sendMessage(event, response)
    }
}