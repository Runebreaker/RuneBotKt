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
        LennyBehavior,
        DxDBehavior,
        DoggoBehaviour
    )

    val messageCommands = listOf<MessageCommandInterface>(
        ConfigCommand,
        TestCommand,
        AdminChannelCommand,
        UsersCommand,
        TagCommand,
        ReminderCommand,
        HelpCommand,
        IntelligentCommand,
        BonkCommand,
        EgalCommand,
        MockCommand,
        ImpostorCommand,
        CollectionCommand,
        RolepingCommand,
        NumbersCommand
    )

    val commandMap = mutableMapOf<String, MessageCommandInterface>()

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

    suspend fun handleMessageCommands(messageCreateEvent: MessageCreateEvent): Boolean
    {
        val args = messageCreateEvent.message.content.split(" ")
        val commandMaybe = args.getOrNull(0) ?: return false
        if (!commandMaybe.startsWith(MessageCommandInterface.prefix)) return false
        val command = commandMap[commandMaybe.removePrefix(MessageCommandInterface.prefix)] ?: return false
        if (command.needsAdmin && !MessageCommandInterface.isAdmin(messageCreateEvent))
        {
            Util.sendMessage(messageCreateEvent, "Only gods may possess such power. You are not worthy.")
            return false
        }
        if (command.isNsfw && !MessageCommandInterface.isNsfw(messageCreateEvent))
        {
            Util.sendMessage(messageCreateEvent, "Gosh, do that somewhere else you pervert.")
            return false
        }
        command.execute(messageCreateEvent, args)
        return true
    }
}