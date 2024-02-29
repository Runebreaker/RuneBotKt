package de.runebot

import de.runebot.behaviors.*
import de.runebot.commands.*
import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.entity.application.GlobalApplicationCommand
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
        EvalCommand,
    )

    private val textCommandMap = mutableMapOf<String, RuneTextCommand>()
    private val slashCommandMap = mutableMapOf<String, RuneSlashCommand>()
    private val messageCommandMap = mutableMapOf<String, RuneMessageCommand>()

    suspend fun prepareCommands(kord: Kord)
    {
        // ensure ids are unique
        if (commands.distinctBy { it.internalId }.size != commands.size)
        {
            throw RuntimeException("Duplicate internal IDs are used: ${commands.map { it.javaClass.name to it.internalId }}")
        }

        val globalAppCommandsInDiscord = mutableListOf<GlobalApplicationCommand>()

        kord.getGlobalApplicationCommands().collect {
            globalAppCommandsInDiscord.add(it)
        }

        // remove commands from config, if they are no longer registered with Discord
        Config.applicationCommands.toList().forEach { (internalId, snowflake) ->
            if (snowflake !in globalAppCommandsInDiscord.map { it.id })
            {
                Config.applicationCommands.remove(internalId)
            }
        }

        // sort commands into types
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
            }

            if (cmd is RuneMessageCommand)
            {
                if (messageCommandMap.containsKey(cmd.name)) error("${cmd.name} is already a registered message command.")
                messageCommandMap[cmd.name] = cmd
            }
        }

        // update/register commands with Discord
        slashCommandMap.forEach { (_, cmd) ->
            // command snowflake already in storage -> send update to Discord
            val snowflake = Config.applicationCommands[cmd.internalId]
            if (snowflake != null)
            {
                kord.rest.interaction.modifyGlobalChatInputApplicationCommand(kord.selfId, snowflake) {
                    cmd.build(this)
                }
            }
            else
            {
                // when registering a new command with Discord, save its Snowflake to bot config
                val newAppCommand = kord.createGlobalChatInputCommand(cmd.name, cmd.description) {
                    cmd.build(this)
                }
                Config.addCommand(cmd.internalId, newAppCommand.id)
            }
        }
        // message commands are overwritten, because it's too complicated otherwise :D
        messageCommandMap.forEach { (_, cmd) ->
            // when registering a new command with Discord, save its Snowflake to bot config
            val newAppCommand = kord.createGlobalMessageCommand(cmd.name) {
                cmd.build(this)
            }
            Config.addCommand(cmd.internalId, newAppCommand.id)

        }

        // remove commands from Discord which should not be there (not in bot config)
        val toDelete = mutableListOf<GlobalApplicationCommand>()
        kord.getGlobalApplicationCommands().collect {
            if (it.id !in Config.applicationCommands.values)
            {
                toDelete.add(it)
            }
        }
        toDelete.forEach { it.delete() }
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