package de.runebot

import de.runebot.behaviors.*
import de.runebot.commands.*
import dev.kord.core.event.message.MessageCreateEvent

object Registry
{
    val behaviors = listOf<Behavior>(
        RedditBehavior,
        TestBehavior,
        TechnikBehavior,
        FBehavior,
        StyleBehavior,
        ListenBehavior,
        DxDBehavior,
    )

    val messageCommands = listOf<MessageCommand>(
        ConfigCommand,
        TestCommand,
        AdminChannelCommand,
        UsersCommand,
        TagCommand
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
        val command = commandMap[commandMaybe.removePrefix(MessageCommand.prefix)] ?: return
        if (command.needsAdmin && !MessageCommand.isAdmin(messageCreateEvent)) return
        command.execute(messageCreateEvent, args)
    }
}