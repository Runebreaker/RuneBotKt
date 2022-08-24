package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object CollectionCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("collection", "col", "c")
    override val shortHelpText: String
        get() = "collection functionality for use with mudae"
    override val longHelpText: String
        get() = "`$commandExample get key`: Gets stored config value for given key.\n" +
                "`$commandExample set key value`: Sets config key to given value."

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