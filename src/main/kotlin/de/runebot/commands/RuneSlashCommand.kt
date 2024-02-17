package de.runebot.commands

import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder

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
    val helpText: String

    /**
     * this method should create the slash command
     */
    suspend fun createCommand(builder: GlobalChatInputCreateBuilder)

    /**
     * what to do, when command is sent
     */
    suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
}