package de.runebot.commands

import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

/**
 * Discord slash command aka chat input command
 */
interface RuneSlashCommand : RuneCommand
{
    /**
     * name of command
     */
    val name: String

    /**
     * short text describing what this command does
     */
    val description: String

    /**
     * this method should define parameters and settings of slash command
     */
    suspend fun build(builder: RootInputChatBuilder)

    /**
     * what to do, when command is sent
     */
    suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
}