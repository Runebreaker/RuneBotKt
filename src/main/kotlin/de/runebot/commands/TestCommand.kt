package de.runebot.commands

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object TestCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("test")
    override val shortHelpText: String
        get() = "for testing"
    override val longHelpText: String
        get() = "`$commandExample`: do stuffs"

    override suspend fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {

    }
}