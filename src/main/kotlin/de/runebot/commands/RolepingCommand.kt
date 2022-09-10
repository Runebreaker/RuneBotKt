package de.runebot.commands

import de.runebot.Util
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

object RolepingCommand : MessageCommandInterface
{
    private val roleping = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(names, Pair("set", "Sets the current channel as admin channel.")),
        { event, args, _ ->
            rolePing(event, args)
        },
        emptyList()
    )

    override val names: List<String>
        get() = listOf("roleping", "rping")
    override val shortHelpText: String
        get() = "pings all people who reacted on the given message"
    override val longHelpText: String
        get() = roleping.toTree().toString()

    private lateinit var kord: Kord

    suspend fun rolePing(event: MessageCreateEvent, args: List<String>)
    {
        var replyMessage: Message? = null
        val reactors = mutableSetOf<User>()
        when (args.size)
        {
            0 -> event.message.referencedMessage?.let { message ->
                replyMessage = Util.getMessageById(event.message.channel, message.id.value.toLong())
            } ?: Util.sendMessage(event, "Please specify a message (ID) or reply to it.")
            else ->
            {
                val input = args.subList(0, args.size).joinToString("").trim()
                try
                {
                    val messageId = input.toLong()
                    replyMessage = Util.getMessageById(event.message.channel, messageId)
                } catch (e: Exception)
                {
                    Util.extractMessageLink(input)?.let { link ->
                        val ids = link.split("/").let { splits -> splits.subList(splits.size - 2, splits.size).map { it.toLong() } }
                        replyMessage = Util.getMessageById(MessageChannelBehavior(Snowflake(ids[0]), kord), ids[1])
                    } ?: Util.sendMessage(event, "Please provide a valid message ID or link.")
                }
            }
        }
        replyMessage?.let { message ->
            message.reactions.forEach { reaction ->
                message.getReactors(reaction.emoji).onEach { user ->
                    println(user.username)
                    if (!user.isBot) reactors.add(user)
                }.collect()
            }
        }
        val finalMessage = StringBuilder("Get over here!${System.lineSeparator()}")
        finalMessage.append(reactors.joinToString(" ") { it.mention })
        Util.sendMessage(event, finalMessage.toString())
    }

    override fun prepare(kord: Kord)
    {
        this.kord = kord
        println("Roleping Command ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        roleping.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }
}