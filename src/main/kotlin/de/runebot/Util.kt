package de.runebot

import de.runebot.database.DB
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.common.entity.optional.OptionalInt
import dev.kord.common.exception.RequestException
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.cache.data.*
import dev.kord.core.entity.Embed
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.GuildButtonInteraction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.addFile
import dev.kord.x.emoji.Emojis
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.random.Random

object Util
{
    //region Constants

    private val connectors = mutableMapOf(
        "L" to '─',
        "R" to '─',
        "U" to '│',
        "D" to '│',
        "UR" to '┌',
        "RD" to '┐',
        "DR" to '└',
        "RU" to '┘',
        "TR" to '├',
        "TL" to '┤',
        "TD" to '┬',
        "TU" to '┴',
    )

    private val messageLinkPattern = Regex("https:\\/\\/discord.com\\/channels(\\/\\d+){3}")

    //endregion

    fun String.randomizeCapitalization(): String
    {
        val stringBuilder = StringBuilder()
        this.forEach { stringBuilder.append(if (Random.nextBoolean()) it.uppercase() else it.lowercase()) }
        return stringBuilder.toString()
    }

    suspend fun sendMessage(event: MessageCreateEvent, message: String): Message?
    {
        if (message.isBlank()) return null
        return event.message.channel.createMessage(message)
    }

    suspend fun sendMessage(channel: MessageChannelBehavior, message: String): Message?
    {
        if (message.isBlank()) return null
        return channel.createMessage(message)
    }

    /**
     * Gets the message of the specified id in given channel. Sends error messages when certain exceptions are thrown.
     * @return The Message object, if found or null otherwise.
     */
    suspend fun getMessageById(channel: MessageChannelBehavior, id: Long): Message?
    {
        try
        {
            return channel.getMessage(Snowflake(id))
        } catch (e: Exception)
        {
            if (e is RequestException) sendMessage(channel, "Something went wrong (probably with the Discord API). Ping my creator if you think this is wrong.")
            if (e is EntityNotFoundException) sendMessage(channel, "Message not found.")
        }
        return null
    }

    /**
     * Gets the message of the specified id in given channel. Sends error messages when certain exceptions are thrown.
     * @return The Message object, if found or null otherwise.
     */
    suspend fun getMessageById(event: MessageCreateEvent, id: Long): Message?
    {
        return getMessageById(event.message.channel, id)
    }

    /**
     * Extracts a message link from the string if possible.
     * @return Returns the message link if found or null otherwise.
     */
    fun extractMessageLink(input: String): String?
    {
        messageLinkPattern.find(input)?.let { return it.value } ?: return null
    }

    suspend fun sendImage(channel: MessageChannelBehavior, path: Path)
    {
        channel.createMessage { this.addFile(path) }
    }

    suspend fun sendImage(channel: MessageChannelBehavior, fileName: String, inputStream: InputStream)
    {
        channel.createMessage {
            this.addFile(fileName, ChannelProvider { inputStream.toByteReadChannel() })
        }
    }

    suspend fun sendImage(channel: MessageChannelBehavior, fileName: String, bufferedImage: BufferedImage)
    {
        val outputStream = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(bufferedImage, "png", outputStream)
        }
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        sendImage(channel, fileName, inputStream)
    }

    fun downloadFromUrl(url: URL, outputName: String)
    {
        url.openStream().use {
            Channels.newChannel(it).use { rbc ->
                FileOutputStream(outputName).use { fos ->
                    fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
                }
            }
        }
    }

    // region bullshit
    suspend fun sendHero(event: MessageCreateEvent)
    {
        val someEmbed = EmbedBuilder().apply {
            title = "this is a title"
            description = "this is a description"
            color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            author {
                name = "An Author"
                icon = "https://static.planetminecraft.com/files/resource_media/screenshot/1406/herobrine_art_head_thumb.jpg"
            }
            url = "http://runebot.de/"
            thumbnail { url = "https://play-lh.googleusercontent.com/g6TibrD-RIOlVjf_oKn2MyqksmTMlRlX3k5tKpPmxt28RB5R3-QmVIahW1YPlwJMZf8G" }
            image = "https://64.media.tumblr.com/00bf93271d005b46bdc8f66dd033603c/tumblr_prcjahPboI1wbsgl7_500.jpg"
            footer {
                icon = "https://64.media.tumblr.com/avatar_ac6d3497c65b_128.pnj"
                text = "My Hero Brine"
            }
            timestamp = Clock.System.now()
            field {
                name = "field 1"
                value = "value 1"
                inline = false
            }
            field {
                name = "field 2"
                value = "value 2"
                inline = true
            }
            field {
                name = "field 3"
                value = "value 3"
                inline = true
            }
        }

        event.message.channel.createMessage { embeds = mutableListOf(someEmbed) }
    }
    // endregion

    fun replaceUsingRuleset(input: String, ruleset: MutableSet<Rule>): Pair<String, Int>
    {
        var result = input
        var replacementCount = 0

        ruleset.forEach { rule ->
            val ruleRegex = Regex(rule.regex)
            ruleRegex.find(input)?.let {
                result = ruleRegex.replace(result, rule.replace)
                replacementCount++
            }
        }

        return Pair(result, replacementCount)
    }

    data class Rule(val regex: String = "", val replace: String = "")

    class StringTree(rootString: String)
    {
        companion object
        {
            lateinit var activeElement: TreeElement
            var final: StringBuilder = StringBuilder()
        }

        init
        {
            activeElement = TreeElement(rootString)
        }

        private val tree = activeElement

        fun getRoot(): TreeElement
        {
            return tree
        }

        fun getTree(): String = tree.toString()

        class TreeElement(val content: String, private val parentElement: TreeElement? = null)
        {
            var parent = parentElement
            private val children = mutableListOf<TreeElement>()

            fun addChild(element: TreeElement): TreeElement
            {
                children.add(element)
                activeElement = element
                return activeElement
            }

            fun addChild(string: String): TreeElement
            {
                val element = TreeElement(string, this)
                children.add(element)
                activeElement = element
                return activeElement
            }

            fun moveUp(): TreeElement
            {
                activeElement = activeElement.parentElement ?: activeElement
                return activeElement
            }

            fun moveToRoot(): TreeElement
            {
                while (activeElement.parent != null)
                {
                    moveUp()
                }
                return activeElement
            }

            private fun collectTree(depth: Int = 0)
            {
                children.forEach { child ->
                    for (i in 0 until depth) final.append("${connectors["D"]}")
                    if (children.last() == child) final.append("${connectors["DR"]}${child.content}${System.lineSeparator()}")
                    else final.append("${connectors["TR"]}${child.content}${System.lineSeparator()}")
                    child.collectTree(depth + 1)
                }
            }

            fun getName(): String
            {
                return this.content
            }

            override fun toString(): String
            {
                final.append(System.lineSeparator())
                final.append("${this.content}${System.lineSeparator()}")
                collectTree()
                final.insert(0, "```")
                final.append("```")
                val returnString = final.toString()
                final.clear()
                return returnString
            }
        }
    }

    /**
     * Complete class for paginated Embeds.
     */
    class EmbedCatalogue
    {
        val catalogue: Catalogue = Catalogue()

        /**
         * Pagination system for CataloguePages.
         */
        data class Catalogue(val pages: MutableList<CataloguePage> = mutableListOf(), var index: Int = 0)
        {
            val defaultActionRow = ActionRowBuilder().apply {
                interactionButton(
                    style = if (index == 0) ButtonStyle.Secondary else ButtonStyle.Success,
                    customId = "left",
                    builder = {
                        emoji = if (index == 0) DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.noEntrySign.unicode).name)
                        else DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.pointLeft.unicode).name)
                    }
                )
                interactionButton(
                    style = if (index == pages.size - 1) ButtonStyle.Secondary else ButtonStyle.Success,
                    customId = "right",
                    builder = {
                        emoji = if (index == pages.size - 1) DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.noEntrySign.unicode).name)
                        else DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.pointRight.unicode).name)
                    }
                )
            }

            fun currentPage(): CataloguePage
            {
                return pages[index]
            }

            fun nextPage(): CataloguePage
            {
                if (index < pages.lastIndex) return pages[++index]
                return currentPage()
            }

            fun previousPage(): CataloguePage
            {
                if (index > 0) return pages[--index]
                return currentPage()
            }

            fun gotoPage(desiredIndex: Int): CataloguePage
            {
                if (desiredIndex in 0..pages.lastIndex) index = desiredIndex
                return currentPage()
            }
        }

        /**
         * A wrapper for EmbedBuilder which also part of the implementation of a pagination system for easy use of discord embeds.
         */
        data class CataloguePage(val embedBuilder: EmbedBuilder = EmbedBuilder(), val files: MutableMap<String, Path> = mutableMapOf())
        {
            var data = EmbedData(
                title = Optional.Missing(),
                type = Optional.Missing(),
                description = Optional.Missing(),
                url = Optional.Missing(),
                timestamp = Optional.Missing(),
                color = OptionalInt.Value(Color(255, 0, 0).rgb),
                footer = Optional.Missing(),
                image = Optional.Missing(),
                thumbnail = Optional.Missing(),
                video = Optional.Missing(),
                provider = Optional.Missing(),
                author = Optional.Missing(),
                fields = Optional.Missing()
            )

            fun setTitle(title: String)
            {
                data = data.copy(title = Optional.Value(title))
            }

            fun setDescription(text: String)
            {
                data = data.copy(description = Optional.Value(text))
            }

            fun setURL(url: String)
            {
                data = data.copy(url = Optional.Value(url))
            }

            fun setColor(r: Int, g: Int, b: Int)
            {
                data = data.copy(color = OptionalInt.Value(Color(r, g, b).rgb))
            }

            fun setFooter(text: String, iconPath: Path? = null)
            {
                val footerData = EmbedFooterData(
                    text = text,
                    iconUrl = iconPath?.let { Optional.Value("attachment://${iconPath.fileName}") } ?: Optional.Missing(),
                    proxyIconUrl = Optional.Missing()
                )
                data = data.copy(footer = Optional.Value(footerData))
                iconPath?.let { files["Icon"] = it }
            }

            fun setImage(path: Path)
            {
                val imageData = EmbedImageData(
                    url = Optional.Value("attachment://${path.fileName}"),
                    proxyUrl = Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(image = Optional.Value(imageData))
                files["Image"] = path
            }

            fun setImageAsURL(url: String)
            {
                val imageData = EmbedImageData(
                    url = Optional.Value(url),
                    proxyUrl = Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(image = Optional.Value(imageData))
            }

            fun setThumbnail(path: Path)
            {
                val thumbnailData = EmbedThumbnailData(
                    url = Optional.Value("attachment://${path.fileName}"),
                    proxyUrl = Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(thumbnail = Optional.Value(thumbnailData))
                files["Thumbnail"] = path
            }

            fun setThumbnailAsURL(url: String)
            {
                val thumbnailData = EmbedThumbnailData(
                    url = Optional.Value(url),
                    proxyUrl = Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(thumbnail = Optional.Value(thumbnailData))
            }

            fun setVideo(url: String)
            {
                val videoData = EmbedVideoData(
                    url = Optional.Value(url),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(video = Optional.Value(videoData))
            }

            fun setProvider(name: String, url: String = "")
            {
                val providerData = EmbedProviderData(
                    name = Optional.Value(name),
                    url = if (url.isNotBlank()) Optional.Value(url) else Optional.Missing()
                )
                data = data.copy(provider = Optional.Value(providerData))
            }

            fun setAuthor(author: String, url: String = "")
            {
                val authorData = EmbedAuthorData(
                    name = Optional.Value(author),
                    url = if (url.isNotBlank()) Optional.Value(url) else Optional.Missing(),
                    iconUrl = Optional.Missing(),
                    proxyIconUrl = Optional.Missing(),
                )
                data = data.copy(author = Optional.Value(authorData))
            }

            fun addFields(vararg field: EmbedFieldData)
            {
                val fields = mutableListOf<EmbedFieldData>()
                data.fields.value?.let {
                    fields.addAll(it)
                }
                fields.addAll(field)
                data = data.copy(fields = Optional.Value(fields))
            }

            /**
             * Applies all changed values to the embedBuilder.
             */
            fun apply()
            {
                RuneBot.kord?.let { kord ->
                    Embed(data, kord).apply(embedBuilder)
                } ?: error("Kord not found.")
            }
        }

        /**
         * Applies the current page of the catalogue to the passed builder.
         */
        fun makeEmbed(builder: MessageBuilder, actionRowBuilder: ActionRowBuilder? = catalogue.defaultActionRow)
        {
            builder.apply {
                val currentPage = catalogue.currentPage()
                embeds = mutableListOf(currentPage.embedBuilder)
                currentPage.files.forEach {
                    addFile(it.value)
                }
                actionRowBuilder?.let { components = mutableListOf(it) }
            }
        }

        /**
         * Changes the page of the catalogue based on the interaction component id. This requires a default action row (createDefaultActionRow) to be present.
         */
        suspend fun changePage(interaction: GuildButtonInteraction, actionRowBuilder: ActionRowBuilder? = catalogue.defaultActionRow)
        {
            interaction.message.edit {
                attachments = mutableListOf()
                files.clear()
                if (interaction.componentId == "left")
                {
                    catalogue.previousPage()
                    makeEmbed(this, actionRowBuilder)
                }
                if (interaction.componentId == "right")
                {
                    catalogue.nextPage()
                    makeEmbed(this, actionRowBuilder)
                }
            }
        }
    }

    // region Puzzles

    enum class PuzzleType(val id: Int)
    {
        INPUT_OUTPUT(0),
        GENERATOR(1);

        companion object
        {
            fun fromId(i: Int): PuzzleType
            {
                return when (i)
                {
                    0 -> INPUT_OUTPUT
                    1 -> GENERATOR
                    else -> INPUT_OUTPUT
                }
            }

            fun PuzzleType.toInfoString(): String
            {
                return when (this)
                {
                    INPUT_OUTPUT -> InputOutputPuzzle(-1, -1).infoString
                    GENERATOR -> TODO()
                }
            }
        }
    }

    enum class PuzzleRewardType(val id: String)
    {
        PUZZLE_POINTS("Puzzle Points"),
        BADGE("Badge");

        companion object
        {
            fun fromId(s: String): PuzzleRewardType
            {
                return when (s)
                {
                    "Puzzle Point(s)" -> PUZZLE_POINTS
                    "Badge(s)" -> BADGE
                    else -> PUZZLE_POINTS
                }
            }
        }
    }

    /**
     * This interface holds core functions of all puzzle.
     */
    interface PuzzleInterface
    {
        fun onStart(userId: Long)
        fun onSolve(userId: Long)
        fun onFail(userId: Long)
        fun onPause(userId: Long)

        /**
         * Decodes the details of the puzzle from a string and applies them.
         */
        fun setDetails(details: String)
    }

    /**
     * This class builds the base of a puzzle, inheriting from PuzzleInterface.
     */
    abstract class Puzzle : PuzzleInterface
    {
        // Functional variables
        abstract val puzzleId: Int
        abstract val creatorId: Long

        // Flavor variables
        abstract val name: String
        abstract val description: String
        abstract val difficulty: Int
        abstract val maxAttempts: Int
        abstract val rewardType: PuzzleRewardType
        abstract val rewardAmount: Int
        abstract val hints: List<String>

        // Puzzle variables
        abstract val type: PuzzleType
        abstract val infoString: String

        // Catalogue
        abstract fun createInfoPage(author: User? = null): EmbedCatalogue.CataloguePage
        abstract fun createPuzzlePage(author: User? = null): EmbedCatalogue.CataloguePage
        fun createHintCatalogue(): EmbedCatalogue
        {
            val catalogue = EmbedCatalogue()
            var index = 1
            hints.forEach {
                val page = EmbedCatalogue.CataloguePage()
                page.setTitle("$puzzleId - $name - Difficulty: $difficulty")
                page.setDescription("HINT $index: $it")
                page.apply()
                catalogue.catalogue.pages.add(page)
                index++
            }
            return catalogue
        }

        // Puzzle functions
        abstract fun checkSolution(submission: String): Boolean
        abstract fun getSolution(): String

        // Instantiate Puzzle depending on type
        companion object
        {
            const val TYPE_DIALOGUE = "What is the type of the puzzle? (0: Input-Output, 1: Generator)"

            fun createPuzzle(
                type: PuzzleType,
                puzzleId: Int,
                creatorId: Long,
                details: String,
                name: String = "Unnamed Puzzle",
                description: String = "Seems a little challenging :)",
                difficulty: Int = 0,
                maxAttempts: Int = 3,
                rewardType: PuzzleRewardType = PuzzleRewardType.PUZZLE_POINTS,
                rewardAmount: Int = 0,
                hints: List<String> = listOf()
            ): Puzzle
            {
                val returnPuzzle = when (type)
                {
                    PuzzleType.INPUT_OUTPUT -> InputOutputPuzzle(puzzleId, creatorId, name, description, difficulty, maxAttempts, rewardType, rewardAmount, hints,
                        onStartImplementation = { userId ->
                            DB.getPuzzleStatsForUser(userId, puzzleId)?.let {
                                if (it.second) return@InputOutputPuzzle
                                DB.addPuzzleAttemptForUser(userId, puzzleId)
                            } ?: DB.storePuzzleStatsForUser(userId, puzzleId, 1, false, 0)
                            DB.setActivePuzzleIdForUser(userId, puzzleId)
                        },
                        onSolveImplementation = { userId ->
                            DB.getPuzzleStatsForUser(userId, puzzleId)?.let { if (it.second) return@InputOutputPuzzle }
                            DB.markPuzzleAsSolvedForUser(userId, puzzleId)
                            DB.setActivePuzzleIdForUser(userId, null)
                        },
                        onFailImplementation = { userId ->
                            DB.getPuzzleStatsForUser(userId, puzzleId)?.let { if (it.second) return@InputOutputPuzzle }
                            DB.addPuzzleAttemptForUser(userId, puzzleId)
                        },
                        onPauseImplementation = { userId ->
                            DB.getPuzzleStatsForUser(userId, puzzleId)?.let { if (it.second) return@InputOutputPuzzle }
                            DB.setActivePuzzleIdForUser(userId, null)
                        }
                    )

                    PuzzleType.GENERATOR -> TODO()
                }
                returnPuzzle.setDetails(details)
                return returnPuzzle
            }
        }
    }

    /**
     * This class describes a simple input -> output puzzle type.
     * @param input The input string.
     * @param output The output string.
     * @param puzzleId The id of the puzzle.
     * @param creatorId The discord id of the creator.
     * @param name The name of the puzzle.
     * @param description The description of the puzzle.
     * @param difficulty The difficulty of the puzzle.
     * @param maxAttempts The maximum amount of attempts.
     * @param rewardType The reward type.
     * @param rewardAmount The reward amount.
     */
    class InputOutputPuzzle(
        override val puzzleId: Int,
        override val creatorId: Long,
        override val name: String = "Unnamed Puzzle",
        override val description: String = "Seems a little challenging :)",
        override val difficulty: Int = 0,
        override val maxAttempts: Int = 3,
        override val rewardType: PuzzleRewardType = PuzzleRewardType.PUZZLE_POINTS,
        override val rewardAmount: Int = 0,
        override val hints: List<String> = listOf(),
        override val type: PuzzleType = PuzzleType.INPUT_OUTPUT,
        val onStartImplementation: (Long) -> Unit = {},
        val onSolveImplementation: (Long) -> Unit = {},
        val onFailImplementation: (Long) -> Unit = {},
        val onPauseImplementation: (Long) -> Unit = {},
        private var input: String = "",
        private var output: String = "",
    ) : Puzzle()
    {
        override val infoString: String = "Please provide the input and output of the puzzle in the following format: (input,output)"

        override fun createInfoPage(author: User?): EmbedCatalogue.CataloguePage
        {
            val page = EmbedCatalogue.CataloguePage()
            page.setTitle("$puzzleId - $name - Difficulty: $difficulty")
            page.setDescription("Maximum Tries: $maxAttempts\n$description")
            page.addFields(
                EmbedFieldData("Maximum Tries", maxAttempts.toString()),
                EmbedFieldData("Description", description, OptionalBoolean.Value(true)),
                EmbedFieldData("Reward Type", rewardType.toString()),
                EmbedFieldData("Reward Amount", rewardAmount.toString(), OptionalBoolean.Value(true)),
            )
            page.setFooter("Puzzle by: ${author?.username ?: "Unknown User"}")
            page.apply()

            return page
        }

        override fun createPuzzlePage(author: User?): EmbedCatalogue.CataloguePage
        {
            val page = EmbedCatalogue.CataloguePage()
            page.setTitle("$puzzleId - $name - Difficulty: $difficulty")
            page.setDescription(input)
            page.setFooter("Puzzle by: ${author?.username ?: "Unknown User"}")
            page.apply()

            return page
        }

        override fun checkSolution(submission: String): Boolean = submission == output
        override fun getSolution(): String = output

        override fun onStart(userId: Long) = onStartImplementation(userId)

        override fun onSolve(userId: Long) = onSolveImplementation(userId)

        override fun onFail(userId: Long) = onFailImplementation(userId)

        override fun onPause(userId: Long) = onPauseImplementation(userId)

        override fun setDetails(details: String) = details.split(",").map { it.trim() }.let {
            input = it[0]
            output = it[1]
        }
    }

    // endregion

    // region Puzzle Creation Menu

    fun createPuzzleCreationMenuActionRow(): ActionRowBuilder
    {
        val actionRow = ActionRowBuilder().apply {
            interactionButton(
                style = ButtonStyle.Primary,
                customId = "create",
                builder = {
                    label = "Create Puzzle!"
                }
            )
        }

        return actionRow
    }

    fun createPuzzleCreationMenu(): EmbedCatalogue
    {
        val creationMenu = EmbedCatalogue()
        val infoPage = EmbedCatalogue.CataloguePage()
        infoPage.setTitle("Puzzle Creation Menu")
        infoPage.setDescription("Click the button to receive a DM, which will guide you through the puzzle creation process.")
        infoPage.apply()
        creationMenu.catalogue.pages.add(infoPage)
        return creationMenu
    }

    // endregion

    data class PuzzleGenericInfo(
        var name: String = "",
        var description: String = "",
        var difficulty: Int = 0,
        var maxAttempts: Int = 0,
        var rewardType: PuzzleRewardType = PuzzleRewardType.PUZZLE_POINTS,
        var rewardAmount: Int = 0,
        var hints: List<String> = emptyList()
    )
    {
        fun reset()
        {
            name = ""
            description = ""
            difficulty = 0
            maxAttempts = 0
            rewardType = PuzzleRewardType.PUZZLE_POINTS
            rewardAmount = 0
            hints = emptyList()
        }
    }
}