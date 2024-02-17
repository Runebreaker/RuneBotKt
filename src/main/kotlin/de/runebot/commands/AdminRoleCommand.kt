package de.runebot.commands

import de.runebot.Util
import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object AdminRoleCommand : RuneTextCommand
{
    override val names: List<String>
        get() = listOf("setadmin", "setadmon", "setgommemode")
    override val shortHelpText: String
        get() = "set admin role id"
    override val longHelpText: String
        get() = "`${commandExample} id`:set admin role id to `id`"

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        Config.setAdminRoleId(event.guildId?.value ?: return, args.getOrNull(1)?.toULongOrNull() ?: return)
        Util.sendMessage(event, "Successfully set adminRoleId to `${args.getOrNull(1)}`")
    }

    override val needsAdmin: Boolean
        get() = true
}