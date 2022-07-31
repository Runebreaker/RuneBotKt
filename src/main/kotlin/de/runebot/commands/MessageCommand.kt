package de.runebot.commands

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

interface MessageCommand
{
    companion object
    {
        val prefix = ">"
    }

    /**
     * This represents the literal by which the command is identified
     */
    val name: String

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