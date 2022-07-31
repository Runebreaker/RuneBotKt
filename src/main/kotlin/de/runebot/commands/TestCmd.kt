package de.runebot.commands

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object TestCmd : MessageCommand
{
    override val name: String
        get() = "test"

    override fun prepare(kord: Kord)
    {
        println("test command prepared")
    }

    override suspend fun execute(event: MessageCreateEvent, args: Array<String>)
    {
        event.message.channel.createMessage("ECHO!")
    }
}