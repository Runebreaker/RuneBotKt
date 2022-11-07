package de.runebot.commands

import de.runebot.Util
import de.runebot.database.DB
import de.runebot.database.DBResponse
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object TagCommand : MessageCommandInterface
{
    private val create = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("create", "cr", "c"), Pair("create <tag name> <message>", "Creates a new tag.")),
        { event, args, _ ->
            event.message.author?.let {
                when (DB.storeTag(args[0], args.subList(1, args.size).joinToString(" "), it.id.value.toLong()))
                {
                    DBResponse.SUCCESS -> Util.sendMessage(event, "Tag created successfully.")
                    else -> Util.sendMessage(event, "Tag couldn't be created.")
                }
            }
        },
        emptyList()
    )
    private val update = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("update", "up", "u"), Pair("update <tag name> <new message>", "Updates a tag, if you own it.")),
        { event, args, _ ->
            event.message.author?.let {
                when (DB.updateTagIfOwner(args[0], args.subList(1, args.size).joinToString(" "), it.id.value.toLong()))
                {
                    DBResponse.SUCCESS -> Util.sendMessage(event, "Tag updated successfully.")
                    DBResponse.FAILURE -> Util.sendMessage(event, "Tag couldn't be updated.")
                    DBResponse.WRONG_USER -> Util.sendMessage(event, "You don't own this tag.")
                    DBResponse.MISSING_ENTRY -> Util.sendMessage(event, "Tag doesn't exist.")
                }
            }
        },
        emptyList()
    )
    private val delete = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("delete", "del", "d"), Pair("delete <tag name>", "Deletes the tag, if you own it.")),
        { event, args, _ ->
            event.message.author?.let {
                when (DB.deleteTagIfOwner(args.subList(0, args.size).joinToString(" "), it.id.value.toLong()))
                {
                    DBResponse.SUCCESS -> Util.sendMessage(event, "Tag deleted successfully.")
                    DBResponse.FAILURE -> Util.sendMessage(event, "Tag couldn't be deleted.")
                    DBResponse.WRONG_USER -> Util.sendMessage(event, "You don't own this tag.")
                    DBResponse.MISSING_ENTRY -> Util.sendMessage(event, "Tag doesn't exist.")
                }
            }
        },
        emptyList()
    )
    private val tag = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("tag", "t"), Pair("tag <tag name>", "Outputs the tag stored under the specified name.")),
        { event, args, _ ->
            event.message.author?.let {
                DB.getTag(args[0])?.let {
                    Util.sendMessage(event, it)
                } ?: Util.sendMessage(event, "Tag not found.")
            }
        },
        listOf(
            create,
            update,
            delete
        )
    )

    override val names: List<String>
        get() = listOf("tag", "t")
    override val shortHelpText: String
        get() = "create/get saved tags"
    override val longHelpText: String
        get() = tag.toTree().toString()
    private lateinit var kord: Kord

    override fun prepare(kord: Kord)
    {
        this.kord = kord
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event, "Try >(`${names.joinToString("` | `")}`) `help`")
            return
        }

        if (names.contains(args[0].substring(1))) tag.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }
}