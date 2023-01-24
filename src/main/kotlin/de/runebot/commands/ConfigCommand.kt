package de.runebot.commands

import de.runebot.Registry
import de.runebot.Util
import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object ConfigCommand : MessageCommandInterface
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

    val behaviourNames: List<String> = Registry.behaviors.map { behavior -> behavior::class.simpleName.toString() }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.getOrNull(1) == "get")
        {
            val key = args.getOrNull(2) ?: return
            event.message.channel.createMessage("`$key` is set to `${Config.getValue(key)}`")
            return
        }
        if (args.getOrNull(1) == "set")
        {
            val key = args.getOrNull(2) ?: return
            val value = args.getOrNull(3) ?: ""
            Config.storeValue(key, value)
            event.message.channel.createMessage("`$key` is set to `${Config.getValue(key)}`")
            return
        }
        if (args.getOrNull(1) == "reset")
        {
            val key = args.getOrNull(2) ?: return
            Config.resetValue(key)
            event.message.channel.createMessage("`$key` has been reset.")
            return
        }
        if (args.getOrNull(1) == "uwu")
        {
            val key = args.getOrNull(2) ?: return
            val value = args.getOrNull(3)

            if (value == null)
            {
                Config.resetRule(key)
                event.message.channel.createMessage("Rule removed.")
            }
            else
            {
                Config.storeRule(key, value)
                event.message.channel.createMessage("Rule made.")
            }
            return
        }
        if (args.getOrNull(1) == "behaviour")
        {
            if (args.getOrNull(2) == "list")
            {
                if (Registry.behaviors.isEmpty())
                {
                    Util.sendMessage(event, "No behaviours found.")
                    return
                }
                val bobTheStringBuilder: StringBuilder = StringBuilder("These are all available behaviours:")
                behaviourNames.forEach {
                    bobTheStringBuilder.append("\n${it}")
                }
                Util.sendMessage(event, bobTheStringBuilder.toString())
                return
            }
            if (args.getOrNull(2) == "disable")
            {
                if (args.size < 4)
                {
                    event.guildId?.let { guildSF ->
                        behaviourNames.forEach {
                            Config.storeDisabledBehaviour(guildSF.value, event.message.channelId.value, it)
                        }
                        Util.sendMessage(event, "Disabled all behaviours.")
                    } ?: Util.sendMessage(event, "Did not find associated guild.")
                    return
                }
                if (!behaviourNames.contains(args[3]))
                {
                    Util.sendMessage(event, "Please specify a valid behaviour. Try '>behaviour list.' or just use '>behaviour disable' to disable all behaviours.")
                    return
                }
                event.guildId?.let { guildSF ->
                    Config.storeDisabledBehaviour(guildSF.value, event.message.channelId.value, args[3])
                    Util.sendMessage(event, "Disabled behaviour ${args[3]}")
                } ?: Util.sendMessage(event, "Did not find associated guild.")
                return
            }
            if (args.getOrNull(2) == "enable")
            {
                if (args.size < 4)
                {
                    event.guildId?.let { guildSF ->
                        behaviourNames.forEach {
                            Config.resetDisabledBehaviour(guildSF.value, event.message.channelId.value, it)
                        }
                        Util.sendMessage(event, "Enabled all behaviours.")
                    } ?: Util.sendMessage(event, "Did not find associated guild.")
                    return
                }
                if (!behaviourNames.contains(args[3]))
                {
                    Util.sendMessage(event, "Please specify a valid behaviour. Try '>behaviour list.' or just use '>behaviour enable' to enable all behaviours.")
                    return
                }
                event.guildId?.let { guildSF ->
                    Config.resetDisabledBehaviour(guildSF.value, event.message.channelId.value, args[3])
                    Util.sendMessage(event, "Enabled behaviour ${args[3]}")
                } ?: Util.sendMessage(event, "Did not find associated guild.")
            }
        }
    }
}