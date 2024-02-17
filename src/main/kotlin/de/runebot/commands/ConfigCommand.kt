package de.runebot.commands

import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object ConfigCommand : RuneMessageCommand
{
    override val names: List<String>
        get() = listOf("config")
    override val shortHelpText: String
        get() = "manage config entries"
    override val longHelpText: String
        get() = "`$commandExample get key`: Gets stored config value for given key.\n" +
                "`$commandExample set key value`: Sets config key to given value.\n" +
                "`$commandExample reset key`: Resets value for given key.\n" +
                "`$commandExample uwu key [value]`: Set uwu rule. Rule is removed if value is empty."

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
            event.message.channel.createMessage("`$key` is set to `${Config.getValue(event.guildId?.value ?: return, key)}`")
            return
        }
        if (args.getOrNull(1) == "set")
        {
            val key = args.getOrNull(2) ?: return
            val value = args.getOrNull(3) ?: ""
            Config.storeValue(event.guildId?.value ?: return, key, value)
            event.message.channel.createMessage("`$key` is set to `${Config.getValue(event.guildId?.value ?: return, key)}`")
            return
        }
        if (args.getOrNull(1) == "reset")
        {
            val key = args.getOrNull(2) ?: return
            Config.resetValue(event.guildId?.value ?: return, key)
            event.message.channel.createMessage("`$key` has been reset.")
            return
        }
        if (args.getOrNull(1) == "uwu")
        {
            val key = args.getOrNull(2) ?: return
            val value = args.getOrNull(3)

            if (value == null)
            {
                Config.resetRule(event.guildId?.value ?: return, key)
                event.message.channel.createMessage("Rule removed.")
            }
            else
            {
                Config.storeRule(event.guildId?.value ?: return, key, value)
                event.message.channel.createMessage("Rule made.")
            }
            return
        }
    }
}