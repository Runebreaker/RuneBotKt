package de.runebot.commands

import de.runebot.Util
import de.runebot.Util.EmbedCatalogue.CataloguePage
import de.runebot.database.DB
import de.runebot.database.DBResponse
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.cache.data.EmbedFieldData
import dev.kord.core.entity.Member
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.toList

object TagCommand : RuneTextCommand
{
    private val create = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("create", "cr", "c"), Pair("create <tag name> <message>", "Creates a new tag.")),
        { event, args, _ ->
            event.message.author?.let {
                val tagName = args[0]
                if (!allowedTagNamePattern.matches(tagName))
                {
                    Util.sendMessage(event, "Tag names can only be composed of letters and digits. This also means, that you can't use formatting for the name!")
                    return@let
                }
                when (DB.storeTag(tagName, args.subList(1, args.size).joinToString(" "), it.id.value.toLong()))
                {
                    DBResponse.SUCCESS -> Util.sendMessage(event, "Tag created successfully.")
                    else -> Util.sendMessage(event, "Tag couldn't be created.")
                }
            } ?: Util.sendMessage(event, "Only Discord Users can use this command.")
        },
        emptyList()
    )
    private val update = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("update", "up", "u"), Pair("update <tag name> <new message>", "Updates a tag, if you own it.")),
        { event, args, _ ->
            event.message.author?.let {
                when (DB.updateTagIfOwner(args[0], args.subList(1, args.size).joinToString(" "), it.id.value.toLong()))
                {
                    DBResponse.SUCCESS -> Util.sendMessage(event, "Tag updated successfully.")
                    DBResponse.FAILURE -> Util.sendMessage(event, "Tag couldn't be updated.")
                    DBResponse.WRONG_USER -> Util.sendMessage(event, "You don't own this tag.")
                    DBResponse.MISSING_ENTRY -> Util.sendMessage(event, "Tag doesn't exist.")
                }
            } ?: Util.sendMessage(event, "Only Discord Users can use this command.")
        },
        emptyList()
    )
    private val delete = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("delete", "del", "d"), Pair("delete <tag name>", "Deletes the tag, if you own it.")),
        { event, args, _ ->
            event.message.author?.let {
                when (DB.deleteTagIfOwner(args.subList(0, args.size).joinToString(" "), it.id.value.toLong()))
                {
                    DBResponse.SUCCESS -> Util.sendMessage(event, "Tag deleted successfully.")
                    DBResponse.FAILURE -> Util.sendMessage(event, "Tag couldn't be deleted.")
                    DBResponse.WRONG_USER -> Util.sendMessage(event, "You don't own this tag.")
                    DBResponse.MISSING_ENTRY -> Util.sendMessage(event, "Tag doesn't exist.")
                }
            } ?: Util.sendMessage(event, "Only Discord Users can use this command.")
        },
        emptyList()
    )
    private val owner = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("owner", "o"), Pair("owner <tag name>", "Shows who owns the tag.")),
        { event, args, _ ->
            DB.getTagOwnerId(args[0])?.let { userId ->
                event.getGuildOrNull()?.getMemberOrNull(Snowflake(userId))?.let { ownerMember ->
                    event.message.channel.createMessage {
                        this.apply {
                            embeds = mutableListOf(createOwnerPage(ownerMember).embedBuilder)
                        }
                    }
                }
            }
        },
        emptyList()
    )
    private val ght = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("ght"), Pair("ght <tag name>", "Outputs the tag stored under the specified name from the GHT DB.")),
        { event, args, _ ->
            DB.getTag(args[0], true)?.let {
                Util.sendMessage(event, it)
            } ?: Util.sendMessage(event, "Tag not found.")
        },
        emptyList()
    )
    private val list = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("list", "l"), Pair("list", "Lists all of your tags.")),
        { event, args, _ ->
            val mentions = event.message.mentionedUsers.toList()
            val authors = mentions.ifEmpty { listOf(event.message.author) }.filterNotNull()

            authors.forEach { user ->
                event.getGuildOrNull()?.getMemberOrNull(Snowflake(user.id.value.toLong()))?.let { ownerMember ->
                    event.message.channel.createMessage {
                        this.apply {
                            embeds = mutableListOf(createOwnerPage(ownerMember).embedBuilder)
                        }
                    }
                }
            }
        },
        emptyList()
    )
    private val tag = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("tag", "t"), Pair("tag <tag name>", "Outputs the tag stored under the specified name.")),
        { event, args, _ ->
            DB.getTag(args[0])?.let {
                Util.sendMessage(event, it)
            } ?: Util.sendMessage(event, "Tag not found.")
        },
        listOf(
            create,
            update,
            delete,
            owner,
            ght,
            list
        )
    )

    override val names: List<String>
        get() = listOf("tag", "t")
    override val shortHelpText: String
        get() = "create/get saved tags"
    override val longHelpText: String
        get() = tag.toTree().toString()
    private lateinit var kord: Kord

    private val allowedTagNamePattern = Regex("[\\w\\däüöß]+")

    override fun prepare(kord: Kord)
    {
        this.kord = kord
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event, "Try `>help ${names.firstOrNull()}`")
            return
        }

        if (names.contains(args[0].substring(1))) tag.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun createOwnerPage(ownerMember: Member): CataloguePage
    {
        val ownerTags = DB.getTagsOfOwner(ownerMember.id.value.toLong())?.joinToString(", ") ?: "There was a problem receiving the owners other tags."
        val ownerPage = CataloguePage()
        ownerPage.setTitle(ownerMember.effectiveName)
        ownerPage.setDescription(ownerMember.username)
        ownerMember.avatar?.let { icon ->
            ownerPage.setThumbnailAsURL(icon.cdnUrl.toUrl())
        } ?: ownerPage.setThumbnailAsURL(ownerMember.defaultAvatar.cdnUrl.toUrl())
        ownerPage.addFields(EmbedFieldData("Owned tags:", ownerTags, OptionalBoolean.Value(false)))
        ownerPage.apply()
        return ownerPage
    }
}