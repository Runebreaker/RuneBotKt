package de.runebot.commands

import de.runebot.Util
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.cache.data.EmojiData
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.Member
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.GlobalMessageCommandCreateBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.imageio.ImageIO
import kotlin.streams.toList
import kotlin.time.Duration.Companion.days


object WhatCommand : RuneTextCommand, RuneMessageCommand
{
    override val names: List<String>
        get() = listOf("what")
    override val shortHelpText: String
        get() = "when people speak bottom"
    override val longHelpText: String
        get() = "`$commandExample index`: Use as reply to message with Emojis in it. `index` determines what Emoji should be used and defaults to 0. `index` is zero-indexed."

    private var emojis = mutableListOf<Emoji>()
    private var lastEmojiUpdate: Long = 0
    private lateinit var kord: Kord

    private val guildEmojiRegex = Regex("<a?:[a-zA-Z0-9_]+:[0-9]+>")

    override fun prepare(kord: Kord)
    {
        this.kord = kord
        loadCurrentEmojiList()
        println("Emojis loaded from https://www.unicode.org/Public/emoji/latest/emoji-test.txt")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        event.message.referencedMessage?.let { referencedMessage ->
            if (System.currentTimeMillis() > lastEmojiUpdate + 1.days.inWholeMilliseconds) loadCurrentEmojiList()

            val content = referencedMessage.content
            val index = args.getOrNull(1)?.toIntOrNull() ?: 0

            // searches the message for emojis
            val foundEmojis = findEmojis(content, event.guildId ?: Snowflake(0))
            if (foundEmojis.isEmpty())
            {
                Util.sendMessage(event, "No Emoji found!")
                return
            }

            val image = generateImage(foundEmojis, index, referencedMessage.getAuthorAsMemberOrNull())
            if (image == null)
            {
                Util.sendMessage(event, "Error generating image.")
                return
            }

            Util.sendImage(event.message.channel, "what.jpg", image)
            return
        }
        // if anything else fails, it must be user-error
        Util.sendMessage(event, "`${HelpCommand.commandExample} ${names.firstOrNull()}`")
    }

    /**
     * @param index what emoji in message is to be used
     * @return null, if no emoji is found
     */
    private suspend fun generateImage(foundEmojis: Map<Int, List<String>>, index: Int, member: Member?): BufferedImage?
    {
        // creates background, should never fail
        val background = withContext(Dispatchers.IO) {
            ImageIO.read(WhatCommand::class.java.getResourceAsStream("/IDSB.jpg"))
        }
        val graphics = background.createGraphics()

        // loads image of emoji
        val emoji = loadEmoji(foundEmojis[index] ?: emptyList())
            ?: return null

        // author avatar is skipped if no image is found
        member?.avatar?.cdnUrl?.let { cdnUrl ->
            ImageIO.read(URI(cdnUrl.toUrl()).toURL())
        }?.let {
            val circleBuffer = BufferedImage(250, 250, BufferedImage.TYPE_INT_ARGB)
            val cropG = circleBuffer.createGraphics()
            cropG.clip(Ellipse2D.Double(0.0, 0.0, 250.0, 250.0))
            cropG.drawImage(it, 0, 0, 250, 250, null)
            graphics.drawImage(circleBuffer, 257 - 125, 289 - 125, 250, 250, null) // face
        }

        graphics.drawImage(emoji, 512 - 60, 275 - 60, 120, 120, null) // emoji in speech bubble
        graphics.drawImage(emoji, 243 - 17, 919 - 17, 33, 33, null) // emoji in response text

        return background
    }

    private fun loadEmoji(possibleURLs: List<String>): BufferedImage?
    {
        possibleURLs.forEach { url ->
            try
            {
                return ImageIO.read(URI(url).toURL())
            } catch (e: Exception)
            {
                e.printStackTrace()
            }
        }

        return null
    }

    /**
     * search text for emojis
     *
     * @return list of order index to URL
     */
    private fun findEmojis(text: String, guildId: Snowflake): Map<Int, List<String>>
    {
        val result = mutableListOf<Pair<Int, String>>()

        emojis.forEach {
            var startIndex = 0
            var indexToEmoji: Pair<Int, String> = 0 to "🍆"
            while (text.findAnyOf(listOf(it.fullEmoji), startIndex = startIndex)?.also { indexToEmoji = it } != null)
            {
                result.add(indexToEmoji)
                val (i, emoji) = indexToEmoji
                startIndex = i + emoji.length
            }
        }

        result.sortByDescending { it.second }
        result.sortBy { it.first }

        val realResult = mutableListOf<Pair<Int, String>>()
        result.map { it.first }.toSet().forEach { i -> realResult.add(result.first { it.first == i }) }

        val indexToShouldRemove = mutableMapOf<Int, Boolean>().withDefault { false }
        realResult.forEach { (index, emoji) ->
            if (!indexToShouldRemove.getValue(index))
            {
                (index + 1 until index + emoji.length).forEach { indexToShouldRemove[it] = true } // lösche überlappende emoji
            }
        }

        val stringIndexToEmojiString = realResult.filter { !indexToShouldRemove.getValue(it.first) }.toMutableList() // actually delete emojis that were determined to be deleted
        stringIndexToEmojiString.toList().forEach { (index, emoji) ->
            getPossibleAlternatives(emoji).forEach { stringIndexToEmojiString.add(index to it.fullEmoji) }
        }

        val stringIndexToURLs = stringIndexToEmojiString.map { it.first to getEmojiURL(it.second) }.toMutableList()

        guildEmojiRegex.findAll(text).forEach { matchResult ->
            matchResult.value.split(":")[2].removeSuffix(">").toLongOrNull()?.let { id ->
                stringIndexToURLs.add(matchResult.range.first to getGuildEmojiURL(Snowflake(id), guildId))
            }
        }

        val toBeReturned = mutableMapOf<Int, List<String>>()
        stringIndexToURLs.groupBy({ it.first }, { it.second }).toList().sortedBy { it.first }.forEachIndexed { index, (key, value) -> toBeReturned[index] = value }
        return toBeReturned
    }

    private fun loadCurrentEmojiList()
    {
        try
        {
            val url = URL("https://www.unicode.org/Public/emoji/latest/emoji-test.txt")
            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000 // timing out in a minute
            val reader = BufferedReader(InputStreamReader(conn.inputStream))

            emojis = reader.readLines()
                .filter { !it.startsWith("#") && it.isNotBlank() }
                .map {
                    Emoji(
                        fullEmoji = it.substringBefore(";").trim().split(" ")
                            .map {
                                try
                                {
                                    it.toInt(16)
                                } catch (_: NumberFormatException)
                                {
                                    println("Error with $it")
                                    0
                                }
                            }.joinToString(separator = "") { Character.toChars(it).joinToString("") },
                        name = it.substringAfter(it.substringAfter("#").trim().split(" ")[1]).trim()
                    )
                }.toMutableList()

            lastEmojiUpdate = System.currentTimeMillis()
            reader.close()
        } catch (e: Exception)
        {
            println("ewwow")
        }
    }

    fun getPossibleAlternatives(emoji: String): List<Emoji>
    {
        val alts = mutableListOf<Emoji>()
        emojis.forEach {
            if (it.fullEmoji == emoji)
            {
                alts.addAll(emojis.filter { inner -> it.name == inner.name })
            }
        }
        return alts.distinctBy { it.fullEmoji }
    }

    fun getEmojiURL(emoji: String): String
    {
        val id = emoji.codePoints().toList()
            .joinToString(separator = "-") { it.toString(16) }
        return "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/$id.png"
    }

    fun getGuildEmojiURL(guildEmojiId: Snowflake, guildId: Snowflake): String
    {
        return GuildEmoji(EmojiData(guildEmojiId, guildId), kord).image.cdnUrl.toUrl()
    }

    fun unicodeEmojiToHex(emoji: String): List<String>
    {
        return emoji.codePoints().toList().map { it.toString(16) }
    }

    override val name: String
        get() = "what"

    override suspend fun createCommand(builder: GlobalMessageCommandCreateBuilder)
    {
        // nothing to declare
    }

    override suspend fun execute(event: MessageCommandInteractionCreateEvent)
    {
        with(event)
        {
            val publicResponse = interaction.deferPublicResponse()

            val content = interaction.target.asMessageOrNull()?.content ?: ""
            val foundEmojis = findEmojis(content, interaction.target.asMessageOrNull()?.getGuildOrNull()?.id ?: Snowflake(0))
            val image = generateImage(foundEmojis, 0, interaction.target.asMessageOrNull()?.getAuthorAsMemberOrNull())
            if (image == null)
            {
                publicResponse.respond { this.content = "error finding emoji" }
                return
            }

            publicResponse.respond {
                Util.respondImage(this, "what.jpg", image)
            }
        }
    }
}

class Emoji(val fullEmoji: String, val name: String)
{
    override fun toString(): String
    {
        return "$fullEmoji $name"
    }
}