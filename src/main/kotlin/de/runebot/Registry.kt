package de.runebot

import de.runebot.behaviors.*
import de.runebot.commands.*
import dev.kord.core.Kord
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

object Registry
{
    val behaviors = listOf<Behavior>(
        Reddit,
        Technik,
        F,
        Style,
        Listen,
        Lenny,
        DxD,
        Doggo,
        SixtyNine,
        Rawr,
    )

    val commands = listOf<RuneCommand>(
        ConfigCommand,
        AdminChannelCommand,
        UsersCommand,
        TagCommand,
        ReminderCommand,
        HelpCommand,
        MockCommand,
        ImpostorCommand,
        CollectionCommand,
        RolepingCommand,
        NumbersCommand,
        WhatCommand,
        UwuifyCommand,
        AnilistCommand,
        FrenchifyCommand,
        AcronymCommand,
        AdminRoleCommand,
        BehaviorCommand,
        EvalCommand,
    )

    val commandMap = mutableMapOf<String, RuneMessageCommand>()

    suspend fun prepareCommands(kord: Kord)
    {
        commands.forEach { cmd ->
            if (cmd is RuneMessageCommand)
            {
                cmd.names.forEach { name ->
                    if (commandMap.containsKey(name)) error("$name is already registered for ${cmd.names}")
                    commandMap[name] = cmd
                }

                cmd.prepare(kord)
            }
            if (cmd is RuneSlashCommand)
            {
                kord.createGlobalChatInputCommand(cmd.name, cmd.helpText) {
                    cmd.createCommand(this)
                }

                kord.on<GuildChatInputCommandInteractionCreateEvent> {
                    cmd.execute(this)
                }
            }
        }
    }

    suspend fun handleBehaviors(messageCreateEvent: MessageCreateEvent)
    {
        val messageContent = messageCreateEvent.message.content
        behaviors.forEach { behavior ->
            messageCreateEvent.guildId?.let { guildSF ->
                if (behavior.isEnabled(guildSF.value, messageCreateEvent.message.channelId.value)) behavior.run(messageContent, messageCreateEvent)
            }
        }
    }

    suspend fun handleMessageCommands(messageCreateEvent: MessageCreateEvent): Boolean
    {
        val args = messageCreateEvent.message.content.split(" ")
        val commandMaybe = args.getOrNull(0) ?: return false
        if (!commandMaybe.startsWith(RuneMessageCommand.prefix)) return false
        val command = commandMap[commandMaybe.removePrefix(RuneMessageCommand.prefix)] ?: return false
        if (command.needsAdmin && !RuneMessageCommand.isAdmin(messageCreateEvent))
        {
            Util.sendMessage(messageCreateEvent, "Only gods may possess such power. You are not worthy.")
            return false
        }
        if (command.isNsfw && !RuneMessageCommand.isNsfw(messageCreateEvent))
        {
            Util.sendMessage(messageCreateEvent, "Gosh, do that somewhere else you pervert.")
            return false
        }
        command.execute(messageCreateEvent, args)
        return true
    }
}