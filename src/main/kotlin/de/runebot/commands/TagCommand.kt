package de.runebot.commands

import de.runebot.Util
import de.runebot.Util.EmbedCatalogue.CataloguePage
import de.runebot.database.DB
import de.runebot.database.DBResponse
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.cache.data.EmbedFieldData
import dev.kord.core.entity.User
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.user
import kotlinx.coroutines.flow.toList

object TagCommand : RuneTextCommand, RuneSlashCommand
{
    private val create = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("create", "cr", "c"), Pair("create <tag name> <message>", "Creates a new tag.")),
        { event, args, _ ->
            event.message.author?.let {
                when (createTag(tagName = args[0], content = args.subList(1, args.size).joinToString(" "), ownerSnowflake = it.id))
                {
                    TagCreateResult.SUCCESS ->
                    {
                        Util.sendMessage(event, "Tag created successfully.")
                    }

                    TagCreateResult.INVALID_NAME ->
                    {
                        Util.sendMessage(event, "Tag names can only be composed of letters and digits. This also means, that you can't use formatting for the name!")
                    }

                    TagCreateResult.DB_ERROR ->
                    {
                        Util.sendMessage(event, "Tag couldn't be created.")
                    }
                }
            } ?: Util.sendMessage(event, "Only Discord Users can use this command.")
        },
        emptyList()
    )
    private val update = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("update", "up", "u"), Pair("update <tag name> <new message>", "Updates a tag, if you own it.")),
        { event, args, _ ->
            event.message.author?.let {
                when (updateTag(args[0], args.subList(1, args.size).joinToString(" "), it.id))
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
                when (deleteTag(args.subList(0, args.size).joinToString(" "), it.id))
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
            getOwnerPage(args[0])?.let { cataloguePage ->
                event.message.channel.createMessage {
                    this.apply {
                        embeds = mutableListOf(cataloguePage.embedBuilder)
                    }
                }
            }
        },
        emptyList()
    )
    private val ght = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("ght"), Pair("ght <tag name>", "Outputs the tag stored under the specified name from the GHT DB.")),
        { event, args, _ ->
            getGhtTag(args[0])?.let {
                Util.sendMessage(event, it)
            } ?: Util.sendMessage(event, "Tag not found.")
        },
        emptyList()
    )
    private val list = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("list", "l"), Pair("list", "Lists all of your tags.")),
        { event, _, _ ->
            val mentions = event.message.mentionedUsers.toList()
            val authors = mentions.ifEmpty { listOf(event.message.author) }.filterNotNull()

            authors.forEach { user ->
                val cataloguePage = generateOwnerPage(user)
                event.message.channel.createMessage {
                    this.apply {
                        embeds = mutableListOf(cataloguePage.embedBuilder)
                    }
                }
            }
        },
        emptyList()
    )
    private val tag = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("tag", "t"), Pair("tag <tag name>", "Outputs the tag stored under the specified name.")),
        { event, args, _ ->
            getTag(args[0])?.let {
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

    private fun getTag(tagName: String): String?
    {
        return DB.getTag(tagName)
    }

    private fun getGhtTag(tagName: String): String?
    {
        return DB.getTag(tagName, fromGhtTags = true)
    }

    private enum class TagCreateResult
    {
        SUCCESS, INVALID_NAME, DB_ERROR
    }

    private fun isValidTagName(tagName: String): Boolean = allowedTagNamePattern.matches(tagName)

    /**
     * @return if tag creation succeeded or why it failed
     */
    private fun createTag(tagName: String, content: String, ownerSnowflake: Snowflake): TagCreateResult
    {
        if (!isValidTagName(tagName))
        {
            return TagCreateResult.INVALID_NAME
        }
        return when (DB.storeTag(tagName, content, ownerSnowflake.value.toLong()))
        {
            DBResponse.SUCCESS -> TagCreateResult.SUCCESS
            else -> TagCreateResult.DB_ERROR
        }
    }

    /**
     * @return if update was successful
     */
    private fun updateTag(tagName: String, content: String, requesterSnowflake: Snowflake): DBResponse
    {
        return DB.updateTagIfOwner(tagName, content, requesterSnowflake.value.toLong())
    }

    /**
     * @return if delete was successful
     */
    private fun deleteTag(tagName: String, requesterSnowflake: Snowflake): DBResponse
    {
        return DB.deleteTagIfOwner(tagName, requesterSnowflake.value.toLong())
    }

    private suspend fun getOwnerPage(id: Snowflake): CataloguePage?
    {
        kord.getUser(id)?.let { owner ->
            return generateOwnerPage(owner)
        }

        return null
    }

    private suspend fun getOwnerPage(tagName: String): CataloguePage?
    {
        DB.getTagOwnerId(tagName)?.let { id ->
            kord.getUser(Snowflake(id))?.let { owner ->
                return generateOwnerPage(owner)
            }
        }

        return null
    }

    private fun generateOwnerPage(ownerUser: User): CataloguePage
    {
        val ownerTags = DB.getTagsOfOwner(ownerUser.id.value.toLong())?.joinToString(", ") ?: "There was a problem receiving the owners other tags."
        val ownerPage = CataloguePage()
        ownerPage.setTitle(ownerUser.effectiveName)
        ownerPage.setDescription(ownerUser.username)
        ownerUser.avatar?.let { icon ->
            ownerPage.setThumbnailAsURL(icon.cdnUrl.toUrl())
        } ?: ownerPage.setThumbnailAsURL(ownerUser.defaultAvatar.cdnUrl.toUrl())
        ownerPage.addFields(EmbedFieldData("Owned tags:", ownerTags, OptionalBoolean.Value(false)))
        ownerPage.apply()
        return ownerPage
    }

    override val name: String
        get() = "tag"
    override val helpText: String
        get() = "get/create text tags"

    override suspend fun createCommand(builder: GlobalChatInputCreateBuilder)
    {
        with(builder)
        {
            subCommand("get", "get text for given tag name") {
                string("name", "name of tag to retrieve") {
                    required = true
                    maxLength = 50
                }
            }
            subCommand("ght", "get text for given GHT tag name") {
                string("name", "name of tag to retrieve") {
                    required = true
                    maxLength = 50
                }
            }
            subCommand("create", "create a new tag") {
                string("name", "name of new tag") {
                    required = true
                    maxLength = 50
                }
                string("content", "content of new tag")
                {
                    required = true
                }
            }
            subCommand("update", "update an existing tag, if you created it") {
                string("name", "name of tag to be edited") {
                    required = true
                    maxLength = 50
                }
                string("content", "new content of this tag")
                {
                    required = true
                }
            }
            subCommand("delete", "delete a tag, if you created it") {
                string("name", "name of tag to retrieve") {
                    required = true
                    maxLength = 50
                }
            }
            subCommand("owner", "determine a tag's owner") {
                string("name", "name of tag to check owner of") {
                    required = true
                    maxLength = 50
                }
            }
            subCommand("list", "list your or someone else's tags") {
                user("user", "list tags of target user") {
                    required = false
                }
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val subcommands = interaction.command.data.options.value?.map { it.name } ?: emptyList()
            when (subcommands.firstOrNull())
            {
                "get" ->
                {
                    val tagName = interaction.command.strings["name"]!!
                    val tag = getTag(tagName)

                    if (tag == null)
                    {
                        interaction.respondEphemeral {
                            content = "tag `$tagName` not found"
                        }
                    }
                    else
                    {
                        interaction.respondPublic {
                            content = "### $tagName:\n" +
                                    "$tag"
                        }
                    }
                }

                "ght" ->
                {
                    val tagName = interaction.command.strings["name"]!!
                    val tag = getGhtTag(tagName)

                    if (tag == null)
                    {
                        interaction.respondEphemeral {
                            content = "GHT tag `$tagName` not found"
                        }
                    }
                    else
                    {
                        interaction.respondPublic {
                            content = "### $tagName:\n" +
                                    "$tag"
                        }
                    }
                }

                "create" ->
                {
                    val tagName = interaction.command.strings["name"]!!
                    val content = interaction.command.strings["content"]!!
                    val ownerSnowflake = interaction.user.id

                    when (createTag(tagName, content, ownerSnowflake))
                    {
                        TagCreateResult.SUCCESS ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag created successfully"
                            }
                        }

                        TagCreateResult.INVALID_NAME ->
                        {
                            interaction.respondEphemeral {
                                this.content = "invalid tag name\ntag names can only be composed of letters and digits"
                            }
                        }

                        TagCreateResult.DB_ERROR ->
                        {
                            interaction.respondEphemeral {
                                this.content = "error creating tag"
                            }
                        }
                    }
                }

                "update" ->
                {
                    val tagName = interaction.command.strings["name"]!!
                    val content = interaction.command.strings["content"]!!
                    val requesterSnowflake = interaction.user.id

                    when (updateTag(tagName, content, requesterSnowflake))
                    {
                        DBResponse.SUCCESS ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag updated successfully"
                            }
                        }

                        DBResponse.FAILURE ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag could not be updated"
                            }
                        }

                        DBResponse.WRONG_USER ->
                        {
                            interaction.respondEphemeral {
                                this.content = "you do not own this tag"
                            }
                        }

                        DBResponse.MISSING_ENTRY ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag does not exist"
                            }
                        }
                    }
                }

                "delete" ->
                {
                    val tagName = interaction.command.strings["name"]!!
                    val requesterSnowflake = interaction.user.id

                    when (deleteTag(tagName, requesterSnowflake))
                    {
                        DBResponse.SUCCESS ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag deleted successfully"
                            }
                        }

                        DBResponse.FAILURE ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag could not be deleted"
                            }
                        }

                        DBResponse.WRONG_USER ->
                        {
                            interaction.respondEphemeral {
                                this.content = "you do not own this tag"
                            }
                        }

                        DBResponse.MISSING_ENTRY ->
                        {
                            interaction.respondEphemeral {
                                this.content = "tag does not exist"
                            }
                        }
                    }
                }

                "owner" ->
                {
                    val tagName = interaction.command.strings["name"]!!
                    getOwnerPage(tagName)?.let { cataloguePage ->
                        interaction.respondEphemeral {
                            embeds = mutableListOf(cataloguePage.embedBuilder)
                        }
                    } ?: interaction.respondEphemeral { content = "error finding owner" }
                }

                "list" ->
                {
                    val user = interaction.command.users["user"] ?: interaction.user

                    val cataloguePage = generateOwnerPage(user)
                    interaction.respondEphemeral {
                        embeds = mutableListOf(cataloguePage.embedBuilder)
                    }
                }

                else -> interaction.respondEphemeral {
                    content = "error"
                } // should not happen
            }
        }
    }
}