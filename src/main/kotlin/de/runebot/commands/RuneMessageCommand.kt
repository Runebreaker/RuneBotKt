package de.runebot.commands

import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalMessageCommandCreateBuilder
import dev.kord.rest.builder.interaction.GlobalMessageCommandModifyBuilder

/**
 * refers to Discord message commands, which can be used when right-clicking a message
 */
interface RuneMessageCommand : RuneCommand
{
    /**
     * name of command
     */
    val name: String

    /**
     * these methods should declare nsfw, admin, etc.
     */
    fun createCommand(builder: GlobalMessageCommandCreateBuilder)
    fun editCommand(builder: GlobalMessageCommandModifyBuilder)

    /**
     * what to do, when command is selected
     */
    suspend fun execute(event: MessageCommandInteractionCreateEvent)
}