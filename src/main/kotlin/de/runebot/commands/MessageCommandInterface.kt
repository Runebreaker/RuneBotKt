package de.runebot.commands

import de.runebot.RuneBot
import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

interface MessageCommandInterface
{
    companion object
    {
        val prefix = ">"

        suspend fun isAdmin(event: MessageCreateEvent): Boolean
        {
            val adminRole = event.getGuild()?.getRoleOrNull(RuneBot.adminRoleSnowflake)
            var valid = false
            event.member?.roles?.collect {
                if (it == adminRole)
                {
                    valid = true
                    return@collect
                }
            }
            return valid
        }

        suspend fun isNsfw(event: MessageCreateEvent): Boolean
        {
            return event.message.channel.fetchChannel().data.nsfw.discordBoolean
        }
    }

    /**
     * This represents the literals by which the command is identified
     */
    val names: List<String>

    /**
     * Short help text shown in command overview
     */
    val shortHelpText: String

    /**
     * Long help text shown for detailed help
     */
    val longHelpText: String

    val commandExample: String
        get() = "${prefix}${names.firstOrNull()}"

    /**
     * This represents, if the command needs admin powers.
     */
    val needsAdmin: Boolean
        get() = false

    /**
     * This represents, if the command needs to be executed in a channel marked NSFW.
     */
    val isNsfw: Boolean
        get() = false

    /**
     * This method will be called after initializing Kord
     */
    fun prepare(kord: Kord)

    /**
     * This method will be run when message starts with cmd sequence + this.name
     * @param event MessageCreateEvent from which this method is called
     * @param args is the message word by word (split by " ")
     */
    suspend fun execute(event: MessageCreateEvent, args: List<String>)

    data class CommandDescription(val names: List<String>, val description: Pair<String, String>)

    data class Subcommand(
        val commandDescription: CommandDescription,
        val function: suspend (MessageCreateEvent, List<String>, List<String>) -> Unit = { event, args, path ->
            Util.sendMessage(event, "Try >help $path ${args[0]}")
        }, val subcommands: List<Subcommand>
    )
    {
        suspend fun execute(event: MessageCreateEvent, args: List<String>, path: List<String>)
        {
            if (args.isEmpty()) function(event, args, path)
            else subcommands.firstOrNull {
                it.commandDescription.names.contains(args[0])
            }?.execute(event, args.subList(1, args.size), listOf(path, listOf(args[0])).flatten()) ?: function(event, args, path)
        }

        fun toTree(): Util.StringTree.TreeElement
        {
            val content = if (this.commandDescription.description.second.isBlank()) this.commandDescription.description.first
            else "${this.commandDescription.description.first} - ${this.commandDescription.description.second}"

            val tree = Util.StringTree.TreeElement(content)
            this.subcommands.map { com -> com.toTree() }.forEach {
                tree.addChild(it).parent = tree
            }
            return tree
        }
    }
}