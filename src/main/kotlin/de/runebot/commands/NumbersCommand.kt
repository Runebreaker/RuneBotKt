package de.runebot.commands

import de.runebot.Util
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.OptionalInt
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.cache.data.*
import dev.kord.core.entity.Embed
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.GuildButtonInteraction
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate
import net.sf.image4j.codec.ico.ICODecoder
import org.jsoup.Jsoup
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.Path


object NumbersCommand : MessageCommandInterface
{
    private val numbers = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(names, Pair("numbers <the sauce>", "Retrieves info about the specified artwork.")),
        { event, args, _ ->
            // sample format: https://nhentai.net/g/<insert number>/
            if (args.isEmpty()) return@Subcommand
            args[0].toIntOrNull()?.let { number ->
                if (!Files.exists(Path(iconPathProcessed))) prepareSiteIcon()
                if (!doujins.containsKey(number) || !Files.isDirectory(Path("${doujinDirectory}/Doujin$number"))) doujins[number] = getNumberInfo(number)
                doujins[number]?.let { doujin ->
                    event.message.channel.createMessage {
                        makeEmbed(this, doujin)
                    }
                }
            } ?: Util.sendMessage(event, "Invalid number.")
        },
        emptyList()
    )

    override val names: List<String>
        get() = listOf("numbers", "n")
    override val shortHelpText: String
        get() = "gives info about *cough* important business"
    override val longHelpText: String
        get() = numbers.toTree().toString()
    override val isNsfw: Boolean
        get() = true
    private lateinit var kord: Kord

    private val doujinDirectory = Path("C:/Programming Projects/runebotkt/src/main/resources/doujins/")
    private val iconDirectory = Path("C:/Programming Projects/runebotkt/src/main/resources/icons/")
    private val doujins = mutableMapOf<Int, Doujin>()
    private const val siteUrl = "https://nhentai.to/"
    private const val iconUrl = "${siteUrl}/favicon.ico"
    private val iconPath = "${iconDirectory}/favicon.ico"
    private val iconPathProcessed = "${iconDirectory}/favicon.png"


    //region Inherited

    override fun prepare(kord: Kord)
    {
        this.kord = kord
        kord.on<GuildButtonInteractionCreateEvent>(consumer = {
            changePage(interaction)
            interaction.respondPublic { }
        })
        println("Numbers command ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        numbers.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    //endregion

    //region Doujin Interaction

    /**
     * Changes the page of the doujin based on the interaction component id
     */
    private suspend fun changePage(interaction: GuildButtonInteraction)
    {
        interaction.message.edit {
            attachments = mutableListOf()
            files = mutableListOf()
            interaction.message.embeds[0].title?.toInt().let { number ->
                doujins[number]?.let { doujin ->
                    if (interaction.componentId == "left")
                    {
                        doujin.previousPage()
                        makeEmbed(this, doujin)
                    }
                    if (interaction.componentId == "right")
                    {
                        doujin.nextPage()
                        makeEmbed(this, doujin)
                    }
                }
            }
        }
    }

    //endregion

    //region Embed/Message Creation

    /**
     * Creates a custom action row and returns its builder.
     * @return The resulting ActionRowBuilder
     */
    private fun createActionRow(index: Int = 1, lastIndex: Int): ActionRowBuilder
    {
        return ActionRowBuilder().apply {
            interactionButton(
                style = if (index == 0) ButtonStyle.Secondary else ButtonStyle.Success,
                customId = "left",
                builder = {
                    emoji = DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.pointLeft.unicode).name)
                }
            )
            interactionButton(
                style = if (index == lastIndex) ButtonStyle.Secondary else ButtonStyle.Success,
                customId = "right",
                builder = {
                    emoji = DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.pointRight.unicode).name)
                }
            )
        }
    }

    /**
     * Applies the current page of the doujin to the passed builder.
     */
    private suspend fun makeEmbed(builder: UserMessageCreateBuilder, doujin: Doujin)
    {
        builder.apply {
            val currentPage = doujin.currentPage()
            embeds.add(currentPage.embedBuilder)
            currentPage.files.forEach {
                addFile(it)
            }
            components.add(createActionRow(doujin.index, doujin.pages.lastIndex))
        }
    }

    /**
     * Applies the current page of the doujin to the passed builder.
     */
    private suspend fun makeEmbed(builder: UserMessageModifyBuilder, doujin: Doujin)
    {
        builder.apply {
            val currentPage = doujin.currentPage()
            embeds = mutableListOf(currentPage.embedBuilder)
            currentPage.files.forEach {
                addFile(it)
            }
            components = mutableListOf(createActionRow(doujin.index, doujin.pages.lastIndex))
        }
    }

    //endregion

    //region Page and Doujin Creation

    /**
     * Creates a doujin based on the passed nhentai numbers.
     * @return The created doujin.
     * @param number The nhentai numbers.
     */
    private fun getNumberInfo(number: Int): Doujin
    {
        val doujinData = scrapeNHentai(number)
        val pageList = mutableListOf<DoujinPage>()
        pageList.add(createInfoPage(number, doujinData))
        for (i in 0 until doujinData.page_number)
        {
            pageList.add(createImagePage(number, doujinData, i))
        }
        return Doujin(
            pageList
        )
    }

    private fun createInfoPage(number: Int, doujinData: DoujinData): DoujinPage
    {
        val fields = mutableListOf<EmbedFieldData>()
        if (doujinData.parodies.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Parodies", doujinData.parodies.joinToString(", ")))
        if (doujinData.characters.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Characters", doujinData.characters.joinToString(", ")))
        if (doujinData.tags.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Tags", doujinData.tags.joinToString(", ")))
        if (doujinData.artists.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Artists", doujinData.artists.joinToString(", ")))
        if (doujinData.groups.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Groups", doujinData.groups.joinToString(", ")))
        if (doujinData.languages.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Languages", doujinData.languages.joinToString(", ")))
        if (doujinData.categories.joinToString(", ").isNotEmpty()) fields.add(EmbedFieldData("Categories", doujinData.categories.joinToString(", ")))
        val footerData = EmbedFooterData(
            text = "$siteUrl <<< ${doujinData.upload_date}",
            iconUrl = Optional.Value("attachment://favicon.png"),
            proxyIconUrl = Optional.Missing()
        )
        val thumbnailData = EmbedThumbnailData(
            url = Optional.Value("attachment://0.png"),
            proxyUrl = Optional.Missing(),
            height = OptionalInt.Missing,
            width = OptionalInt.Missing
        )
        val combinedTitle = doujinData.name
        doujinData.original_name?.let { combinedTitle.plus("${System.lineSeparator()}$it") }
        val data = EmbedData(
            title = Optional.Value("$number"),
            type = Optional.Missing(),
            description = Optional.Value(combinedTitle),
            url = Optional.Value("${siteUrl}g/$number"),
            timestamp = Optional.Missing(),
            color = OptionalInt.Value(Color(255, 0, 0).rgb),
            footer = Optional.Value(footerData),
            image = Optional.Missing(),
            thumbnail = Optional.Value(thumbnailData),
            video = Optional.Missing(),
            provider = Optional.Missing(),
            author = Optional.Missing(),
            fields = Optional.Value(fields)
        )
        val builder = EmbedBuilder()
        Embed(data, kord).apply(builder)
        return DoujinPage(builder, listOf(Path("${doujinDirectory}/Doujin${number}/0.png"), Path(iconPathProcessed)))
    }

    private fun createImagePage(number: Int, doujinData: DoujinData, index: Int): DoujinPage
    {
        val footerData = EmbedFooterData(
            text = "$siteUrl <<< ${doujinData.upload_date}",
            iconUrl = Optional.Value("attachment://favicon.png"),
            proxyIconUrl = Optional.Missing()
        )
        val imageData = EmbedImageData(
            url = Optional.Value("attachment://${index}.png"),
            proxyUrl = Optional.Missing(),
            height = OptionalInt.Missing,
            width = OptionalInt.Missing
        )
        val data = EmbedData(
            title = Optional.Value("$number"),
            type = Optional.Missing(),
            description = Optional.Missing(),
            url = Optional.Value("${siteUrl}g/$number"),
            timestamp = Optional.Missing(),
            color = OptionalInt.Value(Color(255, 0, 0).rgb),
            footer = Optional.Value(footerData),
            image = Optional.Value(imageData),
            thumbnail = Optional.Missing(),
            video = Optional.Missing(),
            provider = Optional.Missing(),
            author = Optional.Missing(),
            fields = Optional.Missing()
        )
        val builder = EmbedBuilder()
        Embed(data, kord).apply(builder)
        return DoujinPage(builder, listOf(Path("${doujinDirectory}/Doujin${number}/${index}.png"), Path(iconPathProcessed)))
    }

    private fun createTestPage(): DoujinPage
    {
        val data = EmbedData(
            title = Optional.Value("This is a test"),
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
        val builder = EmbedBuilder()
        Embed(data, kord).apply(builder)
        return DoujinPage(builder)
    }

    //endregion

    //region Web scraper

    private fun scrapeNHentai(number: Int): DoujinData
    {
        val data = DoujinData()
        Jsoup.connect("${siteUrl}g/$number").get().body().run {
            // Fill Data
            select("h1").first()?.let { data.name = it.html() }
            select("h2").first()?.let { data.original_name = it.html() }
            getElementsByClass("tag-container field-name ").forEach { element ->
                with(element.html()) {
                    when
                    {
                        startsWith("Parodies") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.parodies.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                        startsWith("Characters") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.characters.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                        startsWith("Tags") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.tags.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                        startsWith("Artists") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.artists.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                        startsWith("Groups") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.groups.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                        startsWith("Languages") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.languages.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                        startsWith("Categories") ->
                        {
                            element.firstElementChild()?.let { span ->
                                span.children().forEach { childElement ->
                                    data.categories.add(childElement.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                                }
                            }
                        }
                    }
                }
            }
            val pagesRegex = Regex("\\d+ page")
            val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
            getElementById("info")?.forEach { element ->
                val content = element.html()
                pagesRegex.find(content)?.let { match ->
                    data.page_number = match.value.filter { it.isDigit() }.toInt()
                }
                dateRegex.find(content)?.let { match ->
                    data.upload_date = match.value.toLocalDate()
                }
            }

            // Cache Images -> keep for 1 week or so?
            if (!Files.isDirectory(Path("${doujinDirectory}/Doujin$number"))) Files.createDirectory(Path("${doujinDirectory}/Doujin$number"))
            val thumbnailImageAddress = getElementById("cover")?.firstElementChild()?.firstElementChild()?.attr("src")
            var downloadIndex = 0
            downloadDoujinImages(URL(thumbnailImageAddress), number, downloadIndex++)
            getElementsByClass("thumb-container").forEach { pageElement ->
                pageElement.firstElementChild()?.firstElementChild()?.let { imageElement ->
                    downloadDoujinImages(URL(imageElement.attr("data-src")), number, downloadIndex++)
                }
            }
        }
        return data
    }

    private fun downloadDoujinImages(url: URL, number: Int, index: Int)
    {
        if (Files.exists(Path("${doujinDirectory}/Doujin${number}/${index}.png"))) return
        Util.downloadFromUrl(url, "${doujinDirectory}/Doujin${number}/${index}.png")
    }

    private fun prepareSiteIcon()
    {
        Files.deleteIfExists(Path(iconPath))
        Files.deleteIfExists(Path(iconPathProcessed))
        Util.downloadFromUrl(URL(iconUrl), iconPath)
        val images: List<BufferedImage> = ICODecoder.read(File(iconPath))
        ImageIO.write(images[0], "png", File(iconPathProcessed))
    }

    //endregion

    //region Data Classes

    data class Doujin(val pages: List<DoujinPage>, var index: Int = 0, var doujinData: DoujinData = DoujinData())
    {
        fun currentPage(): DoujinPage
        {
            return pages[index]
        }

        fun nextPage(): DoujinPage
        {
            if (index < pages.lastIndex) return pages[++index]
            return currentPage()
        }

        fun previousPage(): DoujinPage
        {
            if (index > 0) return pages[--index]
            return currentPage()
        }

        fun gotoPage(desiredIndex: Int): DoujinPage
        {
            if (desiredIndex in 0..pages.lastIndex) index = desiredIndex
            return pages[index]
        }
    }

    data class DoujinPage(val embedBuilder: EmbedBuilder, val files: List<Path> = emptyList())

    data class DoujinData(
        var name: String = "Unnamed",
        var original_name: String? = null,
        val parodies: MutableCollection<String> = mutableListOf(),
        val characters: MutableCollection<String> = mutableListOf(),
        val tags: MutableCollection<String> = mutableListOf(),
        val artists: MutableCollection<String> = mutableListOf(),
        val groups: MutableCollection<String> = mutableListOf(),
        val languages: MutableCollection<String> = mutableListOf(),
        val categories: MutableCollection<String> = mutableListOf(),
        var page_number: Int = 0,
        var upload_date: LocalDate = LocalDate.fromEpochDays(0)
    )

    //endregion

}