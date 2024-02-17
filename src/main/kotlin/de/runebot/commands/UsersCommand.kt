package de.runebot.commands

import de.runebot.Util
import de.runebot.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.event.message.MessageCreateEvent

object UsersCommand : RuneTextCommand
{
    override val names: List<String>
        get() = listOf("users", "us")
    override val shortHelpText: String
        get() = "list registered users"
    override val longHelpText: String
        get() = "`$commandExample`: print registered users to admin channel"
    override val needsAdmin: Boolean
        get() = true

    private lateinit var kord: Kord

    override fun prepare(kord: Kord)
    {
        this.kord = kord
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        Config.getValue(event.guildId?.value ?: return, "adminChannel")?.let { channelID ->
            val users = StringBuilder()
            event.getGuildOrNull()?.let { guild ->
                users.append("Users on ${guild.name}")
                guild.members.collect { member ->
                    users.append("\n- ${member.effectiveName} (${member.username})")
                }
            }
            Util.sendMessage(MessageChannelBehavior(Snowflake(channelID), kord), users.toString())
        } ?: Util.sendMessage(event, "Admin channel has not been set yet!")
    }
}