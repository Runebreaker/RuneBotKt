package de.runebot.commands

import de.runebot.Util
import de.runebot.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.event.message.MessageCreateEvent

object AdminChannelCommand : MessageCommand
{
    override val names: List<String>
        get() = listOf("adminchannel", "ac")
    override val shortHelpText: String
        get() = "set/get admin channel"
    override val longHelpText: String
        get() = "`$commandExample`: Sends message to currently set admin channel.\n" +
                "`$commandExample set`: Sets the current channel as admin channel."
    override val needsAdmin: Boolean
        get() = true
    private lateinit var kord: Kord

    override fun prepare(kord: Kord)
    {
        this.kord = kord
        println("Set admin channel command ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1) Config.get("adminChannel")?.let { channelID ->
            Util.sendMessage(MessageChannelBehavior(Snowflake(channelID), kord), "This is the admin channel.")
        } ?: Util.sendMessage(event, "Admin channel has not been set yet!")
        else if (args[1] == "set")
        {
            Config.store("adminChannel", event.message.channel.id.toString())
            Util.sendMessage(event, "Admin channel set!")
        }
    }
}