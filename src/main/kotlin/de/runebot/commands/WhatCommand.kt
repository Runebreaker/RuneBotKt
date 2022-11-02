package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO
import kotlin.streams.toList
import kotlin.time.Duration.Companion.days


object WhatCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("what")
    override val shortHelpText: String
        get() = "when people speak bottom"
    override val longHelpText: String
        get() = "`$commandExample index`: Use as reply to message with Emojis in it. `index` determines what Emoji should be used and defaults to 0. `index` is 0-indexed."

    private var emojis = mutableListOf<Emoji>()
    private var lastEmojiUpdate: Long = 0

    override fun prepare(kord: Kord)
    {
        loadCurrentEmojiList()
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        event.message.referencedMessage?.let { referencedMessage ->
            if (System.currentTimeMillis() > lastEmojiUpdate + 1.days.inWholeMilliseconds) loadCurrentEmojiList()

            val content = referencedMessage.content
            val index = args.getOrNull(1)?.toIntOrNull() ?: 0

            val foundEmojis = mutableListOf<Pair<Int, String>>()

            findEmojis(content).forEach { (index, fullEmoji) ->
                getPossibleAlternatives(fullEmoji).forEach { foundEmojis.add(index to it.fullEmoji) }
            }

            val background = withContext(Dispatchers.IO) {
                ImageIO.read(WhatCommand::class.java.getResourceAsStream("/IDSB.jpg"))
            }
            val graphics = background.createGraphics()
            val emoji = withContext(Dispatchers.IO) {
                foundEmojis.filter { it.first == index }.forEach { (_, fullEmoji) ->
                    try
                    {
                        return@withContext ImageIO.read(URL(getEmojiURL(fullEmoji)))
                    } catch (_: Exception)
                    {
                    }
                }

                return@withContext ImageIO.read(URL(getEmojiURL("🍆")))
            }
            referencedMessage.getAuthorAsMember()?.avatar?.url?.let { url ->
                ImageIO.read(URL(url))
            }?.let {
                val circleBuffer = BufferedImage(250, 250, BufferedImage.TYPE_INT_ARGB)
                val cropG = circleBuffer.createGraphics()
                cropG.clip(Ellipse2D.Double(0.0, 0.0, 250.0, 250.0))
                cropG.drawImage(it, 0, 0, 250, 250, null)
                graphics.drawImage(circleBuffer, 257 - 125, 289 - 125, 250, 250, null) // face
            }
            graphics.drawImage(emoji, 512 - 60, 275 - 60, 120, 120, null) // bubble
            graphics.drawImage(emoji, 243 - 17, 919 - 17, 33, 33, null) // response
            Util.sendImage(event.message.channel, "what.jpg", background)

        } ?: Util.sendMessage(event, "`${HelpCommand.commandExample} ${names.firstOrNull()}`")
    }

    /**
     * @return list of pairs of index and emoji
     */
    private fun findEmojis(text: String): List<Pair<Int, String>>
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

        val actualResult = realResult.filter { !indexToShouldRemove.getValue(it.first) } // actually delete emojis that were determined to be deleted
        println(actualResult)
        return actualResult.mapIndexed { index, pair -> index to pair.second }
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
        return "https://twemoji.maxcdn.com/v/latest/72x72/$id.png"
    }

    fun unicodeEmojiToHex(emoji: String): List<String>
    {
        return emoji.codePoints().toList().map { it.toString(16) }
    }

    @JvmStatic
    fun main(args: Array<String>)
    {
        loadCurrentEmojiList()
        getPossibleAlternatives("#️⃣").map { getEmojiURL(it.fullEmoji) }.forEach { println(it) }
    }
}

class Emoji(val fullEmoji: String, val name: String)
{
    override fun toString(): String
    {
        return "$fullEmoji $name"
    }
}