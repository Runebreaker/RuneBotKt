package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object ImpostorCommand : RuneTextCommand
{
    override val names: List<String>
        get() = listOf("impostor", "sussybaka")
    override val shortHelpText: String
        get() = "list impostors"
    override val longHelpText: String
        get() = "`$commandExample`: lists impostors on this server"

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val impostors = mutableListOf<String>()
        event.getGuildOrNull()?.members?.collect { member ->
            member.roles.collect { role ->
                if (role.name.equals("impostor", ignoreCase = true))
                {
                    impostors.add(member.mention)
                }
            }
        }

        if (impostors.isEmpty())
        {
            Util.sendMessage(event, "No sussy bakas around.")
            return
        }

        Util.sendMessage(event, impostors.joinToString(separator = " was an impostor!\n", postfix = " was an impostor!"))
    }
}