package de.runebot.commands

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object TestCommand : MessageCommand
{
    override val names: List<String>
        get() = listOf("test")

    override fun prepare(kord: Kord)
    {
        println("test command prepared")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {

    }
}