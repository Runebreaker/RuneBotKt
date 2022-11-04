package de.runebot.commands

import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object ConfigCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("config")
    override val shortHelpText: String
        get() = "set/get config entries"
    override val longHelpText: String
        get() = "`$commandExample get key`: Gets stored config value for given key.\n" +
                "`$commandExample set key value`: Sets config key to given value."

    override val needsAdmin: Boolean
        get() = true

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.getOrNull(1) == "get")
        {
            val key = args.getOrNull(2) ?: return
            event.message.channel.createMessage("`$key` is set to `${Config.get(key)}`")
            return
        }
        if (args.getOrNull(1) == "set")
        {
            val key = args.getOrNull(2) ?: return
            val value = args.getOrNull(3) ?: ""
            Config.store(key, value)
            event.message.channel.createMessage("`$key` is set to `${Config.get(key)}`")
            return
        }
        if (args.getOrNull(1) == "rule")
        {
            val key = args.getOrNull(2) ?: return
            val value = args.getOrNull(3) ?: return
            Config.storeRule(key, value)
            event.message.channel.createMessage("Rule made.")
            return
        }
    }
}