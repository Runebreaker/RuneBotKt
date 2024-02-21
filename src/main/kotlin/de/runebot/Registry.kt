package de.runebot

import de.runebot.behaviors.*
import de.runebot.commands.*
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent

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
        PuzzleCommand,
        EvalCommand,
    )

    private val textCommandMap = mutableMapOf<String, RuneTextCommand>()
    private val slashCommandMap = mutableMapOf<String, RuneSlashCommand>()
    private val messageCommandMap = mutableMapOf<String, RuneMessageCommand>()

    suspend fun prepareCommands(kord: Kord)
    {
        commands.forEach { cmd ->
            if (cmd is RuneTextCommand)
            {
                cmd.names.forEach { name ->
                    if (textCommandMap.containsKey(name)) error("$name is already a registered text command.")
                    textCommandMap[name] = cmd
                }

                cmd.prepare(kord)
            }

            if (cmd is RuneSlashCommand)
            {
                if (slashCommandMap.containsKey(cmd.name)) error("${cmd.name} is already a registered slash command.")
                slashCommandMap[cmd.name] = cmd

                kord.createGlobalChatInputCommand(cmd.name, cmd.helpText) {
                    cmd.createCommand(this)
                }
            }

            if (cmd is RuneMessageCommand)
            {
                if (messageCommandMap.containsKey(cmd.name)) error("${cmd.name} is already a registered message command.")
                messageCommandMap[cmd.name] = cmd

                kord.createGlobalMessageCommand(cmd.name) {
                    cmd.createCommand(this)
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

    suspend fun handleTextCommands(messageCreateEvent: MessageCreateEvent): Boolean
    {
        val args = messageCreateEvent.message.content.split(" ")
        val commandMaybe = args.getOrNull(0) ?: return false
        if (!commandMaybe.startsWith(RuneTextCommand.prefix)) return false
        val command = textCommandMap[commandMaybe.removePrefix(RuneTextCommand.prefix)] ?: return false
        if (command.needsAdmin && !RuneTextCommand.isAdmin(messageCreateEvent))
        {
            Util.sendMessage(messageCreateEvent, "Only gods may possess such power. You are not worthy.")
            return false
        }
        if (command.isNsfw && !RuneTextCommand.isNsfw(messageCreateEvent))
        {
            Util.sendMessage(messageCreateEvent, "Gosh, do that somewhere else you pervert.")
            return false
        }
        command.execute(messageCreateEvent, args)
        return true
    }

    suspend fun handleSlashCommands(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val cmd = slashCommandMap[interaction.invokedCommandName]
            cmd?.execute(this)
        }
    }

    suspend fun handleMessageCommands(event: MessageCommandInteractionCreateEvent)
    {
        with(event)
        {
            val cmd = messageCommandMap[interaction.invokedCommandName]
            cmd?.execute(this)
        }
    }
}