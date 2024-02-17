package de.runebot.commands

import de.runebot.Registry
import de.runebot.Util
import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object BehaviorCommand : RuneMessageCommand
{
    override val needsAdmin: Boolean
        get() = true
    override val names: List<String>
        get() = listOf("behavior", "behaviour")
    override val shortHelpText: String
        get() = "manages enabled behaviors for current channel"
    override val longHelpText: String
        get() = "All commands only configure for current channel:${System.lineSeparator()}" +
                "`$commandExample enable( <behaviorName>)+`: enables given behavior(s).${System.lineSeparator()}" +
                "`$commandExample disable( <behaviorName>)+`: disables given behavior(s).${System.lineSeparator()}" +
                "`$commandExample enableAll`: enables all behaviors.${System.lineSeparator()}" +
                "`$commandExample disableAll`: disables all behaviors.${System.lineSeparator()}" +
                "`$commandExample list`: lists all available behaviors.${System.lineSeparator()}" +
                "`$commandExample listEnabled`: lists all enabled behaviors."

    val behaviorNames: Set<String>
        get() = Registry.behaviors.map { behavior -> behavior::class.simpleName.toString() }.toSet()


    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val guild = event.guildId?.value ?: return
        val channel = event.message.channelId.value

        when (args[1])
        {
            "enable" ->
            {
                val possibleBehaviors = args.subList(2, args.size).map { it.lowercase() }
                if (possibleBehaviors.isEmpty())
                {
                    Util.sendMessage(event, "`${HelpCommand.commandExample} ${names.first()}`")
                    return
                }

                val enabled = mutableSetOf<String>()

                behaviorNames.forEach { behaviorName ->
                    if (possibleBehaviors.contains(behaviorName.lowercase()))
                    {
                        Config.enableBehavior(guild, channel, behaviorName)
                        enabled.add(behaviorName)
                    }
                }

                Util.sendMessage(event, enabled.joinToString(prefix = "Enabled the following in this channel:", separator = ", ") { "`$it`" })
            }

            "disable" ->
            {
                val possibleBehaviors = args.subList(2, args.size).map { it.lowercase() }
                if (possibleBehaviors.isEmpty())
                {
                    Util.sendMessage(event, "`${HelpCommand.commandExample} ${names.first()}`")
                    return
                }

                val disabled = mutableSetOf<String>()

                behaviorNames.forEach { behaviorName ->
                    if (possibleBehaviors.contains(behaviorName.lowercase()))
                    {
                        Config.disableBehavior(guild, channel, behaviorName)
                        disabled.add(behaviorName)
                    }
                }

                Util.sendMessage(event, disabled.joinToString(prefix = "Disabled the following in this channel:", separator = ", ") { "`$it`" })
            }

            "enableAll" ->
            {
                Config.enableAllBehaviors(guild, channel)
                Util.sendMessage(event, "Enabled all behaviors in this channel.")
            }

            "disableAll" ->
            {
                Config.disableAllBehaviors(guild, channel)
                Util.sendMessage(event, "Disabled all behaviors in this channel.")
            }

            "list" ->
            {
                Util.sendMessage(
                    event,
                    behaviorNames
                        .joinToString(prefix = "Available behaviors:${System.lineSeparator()}", separator = System.lineSeparator()) { "`$it`" }
                )
            }

            "listEnabled" ->
            {
                Util.sendMessage(
                    event,
                    Config.getEnabledBehaviors(guild, channel)
                        .joinToString(prefix = "Enabled behaviors in this channel:${System.lineSeparator()}", separator = System.lineSeparator()) { "`$it`" }
                )
            }

            else -> Util.sendMessage(event, "`${HelpCommand.commandExample} ${names.first()}`")
        }
    }
}