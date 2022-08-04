package de.runebot

import de.runebot.behaviors.Behavior
import de.runebot.behaviors.FBehavior
import de.runebot.behaviors.TestBehavior
import de.runebot.commands.ConfigCommand
import de.runebot.commands.MessageCommand
import de.runebot.commands.TestCommand
import dev.kord.core.event.message.MessageCreateEvent

object Registry
{
    val behaviors = listOf<Behavior>(
        TestBehavior,
        FBehavior
    )

    val messageCommands = listOf<MessageCommand>(
        ConfigCommand,
        TestCommand
    )

    val commandMap = mutableMapOf<String, MessageCommand>()

    init
    {
        messageCommands.forEach { cmd ->
            cmd.names.forEach { name -> commandMap[name] = cmd }
        }
    }

    suspend fun handleBehaviors(messageCreateEvent: MessageCreateEvent)
    {
        val messageContent = messageCreateEvent.message.content
        behaviors.forEach { it.run(messageContent, messageCreateEvent) }
    }

    suspend fun handleMessageCommands(messageCreateEvent: MessageCreateEvent)
    {
        val args = messageCreateEvent.message.content.split(" ")
        val commandMaybe = args.getOrNull(0) ?: return
        if (!commandMaybe.startsWith(MessageCommand.prefix)) return
        commandMap[commandMaybe.removePrefix(MessageCommand.prefix)]?.execute(messageCreateEvent, args) ?: return
    }
}