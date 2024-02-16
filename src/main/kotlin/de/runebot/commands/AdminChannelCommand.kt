package de.runebot.commands

import de.runebot.Util
import de.runebot.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.event.message.MessageCreateEvent

object AdminChannelCommand : MessageCommandInterface
{
    private val set = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("set", "s"), Pair("set", "Sets the current channel as admin channel.")),
        { event, args, _ ->
            Config.storeValue(event.guildId?.value ?: return@Subcommand, "adminChannel", event.message.channel.id.toString())
            Util.sendMessage(event, "Admin channel set!")
        },
        emptyList()
    )
    private val adminchannel = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(names, Pair("adminchannel", "Sends a message in the currently set admin channel.")),
        { event, args, _ ->
            Config.getValue(event.guildId?.value ?: return@Subcommand, "adminChannel")?.let { channelID ->
                Util.sendMessage(MessageChannelBehavior(Snowflake(channelID), kord), "This is the admin channel.")
            } ?: Util.sendMessage(event, "Admin channel has not been set yet!")
        },
        listOf(
            set
        )
    )

    override val names: List<String>
        get() = listOf("adminchannel", "ac")
    override val shortHelpText: String
        get() = "set/get admin channel"
    override val longHelpText: String
        get() = adminchannel.toTree().toString()
    override val needsAdmin: Boolean
        get() = true
    private lateinit var kord: Kord

    override suspend fun prepare(kord: Kord)
    {
        this.kord = kord
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (names.contains(args[0].substring(1))) adminchannel.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }
}