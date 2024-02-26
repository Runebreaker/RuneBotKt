package de.runebot.commands

import de.runebot.Util
import de.runebot.Util.EmbedCatalogue
import de.runebot.Util.EmbedCatalogue.Catalogue
import de.runebot.Util.EmbedCatalogue.CataloguePage
import de.runebot.database.DB
import de.runebot.database.DBResponse
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.cache.data.EmbedFieldData
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.GuildButtonInteraction
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.message.addFile
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.modify.UserMessageModifyBuilder
import dev.kord.x.emoji.Emojis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDate
import net.sf.image4j.codec.ico.ICODecoder
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.Path


object NumbersCommand : RuneTextCommand
{
    private val clear = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("clear", "clr"), Pair("clear", "Deletes all stored doujins.")),
        { event, args, _ ->
            when (DB.deleteAllDoujins())
            {
                DBResponse.SUCCESS -> Util.sendMessage(event, "All doujins deleted.")
                else -> Util.sendMessage(event, "Couldn't delete doujins in the DB.")
            }
            doujins.clear()
        },
        emptyList()
    )

    private val numbers = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(names, Pair("numbers <the sauce>", "Retrieves info about the specified artwork.")),
        { event, args, _ ->
            // sample format: https://nhentai.net/g/<insert number>/
            if (args.isEmpty()) return@Subcommand
            args[0].toIntOrNull()?.let { number ->
                if (!Files.exists(Path(iconPathProcessed))) prepareSiteIcon()
                if (!doujins.containsKey(number) || !Files.isDirectory(Path("${doujinDirectory}/Doujin$number")))
                {
                    getNumberInfo(number, event)?.let {
                        doujins[number] = it.second
                        it.first.saveToDB(number)
                    } ?: run {
                        Util.sendMessage(event, "This doujin does not exist!")
                        return@let
                    }
                }
                doujins[number]?.let { doujin ->
                    event.message.channel.createMessage {
                        makeEmbed(this, doujin)
                    }
                }
            } ?: Util.sendMessage(event, "Invalid number.")
        },
        listOf(
            clear
        )
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

    private val doujinDirectory = Path("doujins/")
    private val iconDirectory = Path("icons/")
    private const val siteUrl = "https://nhentai.to/"
    private const val iconUrl = "${siteUrl}/favicon.ico"
    private val iconPath = "${iconDirectory}/favicon.ico"
    private val iconPathProcessed = "${iconDirectory}/favicon.png"

    private val doujins = mutableMapOf<Int, Catalogue>()


    //region Inherited

    override fun prepare(kord: Kord)
    {
        // Prep kord behaviour for events
        this.kord = kord
        kord.on<GuildButtonInteractionCreateEvent>(consumer = {
            changePage(interaction)
            interaction.respondPublic { }
        })

        // Load saved doujins from DB
//        DB.getAllDoujins().forEach {
//            doujins[it.key] = loadNumberInfo(it.key, it.value).second
//        }

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
            files.clear()
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
    fun createActionRow(index: Int = 1, lastIndex: Int): ActionRowBuilder
    {
        return ActionRowBuilder().apply {
            interactionButton(
                style = if (index == 0) ButtonStyle.Secondary else ButtonStyle.Success,
                customId = "left",
                builder = {
                    emoji = if (index == 0) DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.noEntrySign.unicode).name)
                    else DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.pointLeft.unicode).name)
                }
            )
            interactionButton(
                style = if (index == lastIndex) ButtonStyle.Secondary else ButtonStyle.Success,
                customId = "right",
                builder = {
                    emoji = if (index == lastIndex) DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.noEntrySign.unicode).name)
                    else DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.pointRight.unicode).name)
                }
            )
        }
    }

    /**
     * Applies the current page of the doujin to the passed builder.
     */
    private suspend fun makeEmbed(builder: UserMessageCreateBuilder, doujin: Catalogue)
    {
        builder.apply {
            val currentPage = doujin.currentPage()
            embeds = mutableListOf(currentPage.embedBuilder)
            currentPage.files.forEach {
                addFile(it.value)
            }
            components = mutableListOf(createActionRow(doujin.index, doujin.pages.lastIndex))
        }
    }

    /**
     * Applies the current page of the doujin to the passed builder.
     */
    private suspend fun makeEmbed(builder: UserMessageModifyBuilder, doujin: Catalogue)
    {
        builder.apply {
            val currentPage = doujin.currentPage()
            embeds = mutableListOf(currentPage.embedBuilder)
            currentPage.files.forEach {
                addFile(it.value)
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
    private suspend fun getNumberInfo(number: Int, event: MessageCreateEvent): Pair<DoujinData, Catalogue>?
    {
        val wrapper = EmbedCatalogue()
        val preparationMessage = Util.sendMessage(event, "Preparing doujin... please wait.")
        val doujinData = scrapeNHentai(number, preparationMessage) ?: return null

        // Info Page
        wrapper.catalogue.pages.add(createInfoPage(number, doujinData))
        for (i in 0 until doujinData.page_number)
        {
            wrapper.catalogue.pages.add(createImagePage(number, doujinData, i))
        }
        return Pair(doujinData, wrapper.catalogue)
    }

    /**
     * Loads an already saved doujin.
     * @return The loaded doujin.
     * @param number The nhentai numbers.
     * @param data The data of the saved doujin.
     */
    private fun loadNumberInfo(number: Int, data: DoujinData): Pair<DoujinData, Catalogue>
    {
        val wrapper = EmbedCatalogue()
        val doujinData = data

        // Info Page
        wrapper.catalogue.pages.add(createInfoPage(number, doujinData))
        for (i in 0 until doujinData.page_number)
        {
            wrapper.catalogue.pages.add(createImagePage(number, doujinData, i))
        }
        return Pair(doujinData, wrapper.catalogue)
    }

    /**
     * Creates a page containing the info of the doujin.
     */
    private fun createInfoPage(number: Int, data: DoujinData): CataloguePage
    {
        val page = CataloguePage()

        // Processing values
        val combinedTitle = data.name
        data.original_name?.let { combinedTitle.plus("${System.lineSeparator()}$it") }

        // Applying them
        page.setTitle("$number")
        page.setDescription(combinedTitle)
        page.setURL("${siteUrl}g/$number")
        page.setFooter("$siteUrl <<< ${data.upload_date}", Path(iconPathProcessed))
        page.setThumbnail(Path("${doujinDirectory}/Doujin${number}/0.png"))
        if (data.parodies.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Parodies", data.parodies.joinToString(", ")))
        if (data.characters.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Characters", data.characters.joinToString(", ")))
        if (data.tags.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Tags", data.tags.joinToString(", ")))
        if (data.artists.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Artists", data.artists.joinToString(", ")))
        if (data.groups.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Groups", data.groups.joinToString(", ")))
        if (data.languages.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Languages", data.languages.joinToString(", ")))
        if (data.categories.joinToString(", ").isNotEmpty()) page.addFields(EmbedFieldData("Categories", data.categories.joinToString(", ")))

        page.apply()
        return page
    }

    /**
     * Creates a page featuring the image of a specified page (index) in the source material.
     */
    private fun createImagePage(number: Int, data: DoujinData, index: Int): CataloguePage
    {
        val page = CataloguePage()

        // Applying values
        page.setTitle("$number")
        page.setURL("${siteUrl}g/$number")
        page.setFooter("$siteUrl <<< ${data.upload_date}", Path(iconPathProcessed))
        page.setImage(Path("${doujinDirectory}/Doujin${number}/${index}.png"))

        page.apply()
        return page
    }

    //endregion

    //region Web scraper

    private suspend fun scrapeNHentai(number: Int, msg: Message? = null): DoujinData?
    {
        val data = DoujinData()
        val response = Jsoup.connect("${siteUrl}g/$number").ignoreHttpErrors(true).execute()

        if (response.statusCode() != 200) return null
        response.parse().body().run {
            // Fill Data
            select("h1").first()?.let { data.name = it.html() }
            select("h2").first()?.let { data.original_name = it.html() }
            getElementsByClass("tag-container field-name ").forEach { element ->
                with(element.html()) {
                    when
                    {
                        startsWith("Parodies") -> addHTMLElementStringToCollection(element, data.parodies)
                        startsWith("Characters") -> addHTMLElementStringToCollection(element, data.characters)
                        startsWith("Tags") -> addHTMLElementStringToCollection(element, data.tags)
                        startsWith("Artists") -> addHTMLElementStringToCollection(element, data.artists)
                        startsWith("Groups") -> addHTMLElementStringToCollection(element, data.groups)
                        startsWith("Languages") -> addHTMLElementStringToCollection(element, data.languages)
                        startsWith("Categories") -> addHTMLElementStringToCollection(element, data.categories)
                    }
                }
            }
            select("a[href='#']").first()?.firstElementChild()?.let {
                data.page_number = it.html().toIntOrNull() ?: 0
            }
            val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
            getElementById("info")?.forEach { element ->
                dateRegex.find(element.html())?.let { match ->
                    data.upload_date = match.value.toLocalDate()
                }
            }
            // Cache Images -> keep for 1 week or so? (currently until restart)
            if (!Files.isDirectory(Path("$doujinDirectory"))) Files.createDirectory(Path("$doujinDirectory"))
            if (!Files.isDirectory(Path("${doujinDirectory}/Doujin$number"))) Files.createDirectory(Path("${doujinDirectory}/Doujin$number"))
            val thumbnailImageAddress = getElementById("cover")?.firstElementChild()?.firstElementChild()?.attr("src")
            var downloadIndex = 0
            downloadDoujinImage(URL(thumbnailImageAddress), number, downloadIndex++)
            msg?.edit {
                content = "Downloading pages. This may take a while."
            }
            getElementsByClass("thumb-container").forEach { pageElement ->
                pageElement.firstElementChild()?.firstElementChild()?.let { imageElement ->
                    val thumbnailURL = imageElement.attr("data-src")
                    // Convert .../.../1t.jpg -> .../.../1.jpg
                    val imageURL = Regex("t(?=\\.\\w+)").replace(thumbnailURL, "")

                    downloadDoujinImage(URL(imageURL), number, downloadIndex++)
                }
            }
            msg?.delete("Preparation finished.")
        }
        return data
    }

    private fun addHTMLElementStringToCollection(element: Element, collection: MutableCollection<String>)
    {
        element.firstElementChild()?.let { span ->
            span.children().forEach { childElement ->
                childElement.firstElementChild()?.let { nameChild ->
                    collection.add(nameChild.html().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                }
            }
        }
    }

    private fun downloadDoujinImage(url: URL, number: Int, index: Int)
    {
        if (Files.exists(Path("${doujinDirectory}/Doujin${number}/${index}.png"))) return
        Util.downloadFromUrl(url, "${doujinDirectory}/Doujin${number}/${index}.png")
    }

    private fun prepareSiteIcon()
    {
        if (!Files.isDirectory(iconDirectory)) Files.createDirectory(iconDirectory)
        Files.deleteIfExists(Path(iconPath))
        Files.deleteIfExists(Path(iconPathProcessed))
        Util.downloadFromUrl(URL(iconUrl), iconPath)
        val images: List<BufferedImage> = ICODecoder.read(File(iconPath))
        ImageIO.write(images[0], "png", File(iconPathProcessed))
    }

    //endregion

    //region Data Classes

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
    {
        companion object
        {
            fun loadFromDB(number: Int): DoujinData?
            {
                return DB.getDoujin(number)
            }
        }

        /**
         * Saves current DB.
         * @param number The source numbers to save the content under.
         */
        fun saveToDB(number: Int)
        {
            DB.storeDoujin(
                number,
                name,
                original_name ?: "",
                parodies,
                categories,
                tags,
                artists,
                groups,
                languages,
                categories,
                page_number,
                upload_date.toJavaLocalDate()
            )
        }
    }

    //endregion
}