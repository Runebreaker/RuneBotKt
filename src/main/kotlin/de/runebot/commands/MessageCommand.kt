package de.runebot.commands

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

interface MessageCommand
{
    companion object
    {
        val prefix = ">"

        suspend fun isAdmin(event: MessageCreateEvent): Boolean
        {
            val adminRole = event.getGuild()?.getRoleOrNull(Snowflake(902174999891288124))
            var valid = false
            event.member?.roles?.collect {
                if (it == adminRole)
                {
                    valid = true
                    return@collect
                }
            }
            return valid
        }
    }

    /**
     * This represents the literal by which the command is identified
     */
    val name: String

    /**
     * This represents, if the command needs admin powers.
     */
    val needsAdmin: Boolean
        get() = false

    /**
     * This method will be called after initializing Kord
     */
    fun prepare(kord: Kord)

    /**
     * This method will be run when message starts with cmd sequence + this.name
     * @param event MessageCreateEvent from which this method is called
     * @param args is the message word by word (split by " ")
     */
    suspend fun execute(event: MessageCreateEvent, args: Array<String>)
}