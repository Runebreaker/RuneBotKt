package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object CollectionCommand: MessageCommand
{
    override val names: List<String>
        get() = listOf("collection", "col", "c")

    override fun prepare(kord: Kord)
    {
        println("Collection command ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event,"Try >(`${names.joinToString("` | `")}`) `help`")
            return
        }
    }
}