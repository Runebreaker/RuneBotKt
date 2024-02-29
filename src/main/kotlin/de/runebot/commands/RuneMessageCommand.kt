package de.runebot.commands

import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalMessageCommandCreateBuilder

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
     * this method should declare nsfw, admin, etc.
     */
    suspend fun build(builder: GlobalMessageCommandCreateBuilder)

    /**
     * what to do, when command is selected
     */
    suspend fun execute(event: MessageCommandInteractionCreateEvent)
}