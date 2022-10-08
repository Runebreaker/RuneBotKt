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

    private var emojis = mutableListOf<String>()
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

            val foundEmojis = findEmojis(content)

            val background = withContext(Dispatchers.IO) {
                ImageIO.read(WhatCommand::class.java.getResourceAsStream("/IDSB.jpg"))
            }
            val graphics = background.createGraphics()
            val emoji = withContext(Dispatchers.IO) {
                ImageIO.read(URL(getEmojiURL(foundEmojis.getOrElse(index) { 0 to "🍆" }.second)))
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
            while (text.findAnyOf(listOf(it), startIndex = startIndex)?.also { indexToEmoji = it } != null)
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
        return actualResult
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
                    it.substringBefore(";").trim().split(" ")
                        .map {
                            try
                            {
                                it.toInt(16)
                            } catch (_: NumberFormatException)
                            {
                                println("Error with $it")
                                0
                            }
                        }.joinToString(separator = "") { Character.toChars(it).joinToString("") }
                }.toMutableList()

            emojis.add("\uD83C\uDDE6")
            emojis.add("\uD83C\uDDE7")
            emojis.add("\uD83C\uDDE8")
            emojis.add("\uD83C\uDDE9")
            emojis.add("\uD83C\uDDEA")
            emojis.add("\uD83C\uDDEB")
            emojis.add("\uD83C\uDDEC")
            emojis.add("\uD83C\uDDED")
            emojis.add("\uD83C\uDDEE")
            emojis.add("\uD83C\uDDEF")
            emojis.add("\uD83C\uDDF0")
            emojis.add("\uD83C\uDDF1")
            emojis.add("\uD83C\uDDF2")
            emojis.add("\uD83C\uDDF3")
            emojis.add("\uD83C\uDDF4")
            emojis.add("\uD83C\uDDF5")
            emojis.add("\uD83C\uDDF6")
            emojis.add("\uD83C\uDDF7")
            emojis.add("\uD83C\uDDF8")
            emojis.add("\uD83C\uDDF9")
            emojis.add("\uD83C\uDDFA")
            emojis.add("\uD83C\uDDFB")
            emojis.add("\uD83C\uDDFC")
            emojis.add("\uD83C\uDDFD")
            emojis.add("\uD83C\uDDFE")
            emojis.add("\uD83C\uDDFF")

            lastEmojiUpdate = System.currentTimeMillis()
            reader.close()
        } catch (e: Exception)
        {
            println("ewwow")
        }
    }

    fun getEmojiURL(emoji: String): String // this may be replaced by some js code from twemoji: https://twemoji.maxcdn.com/v/latest/twemoji.min.js ? https://github.com/twitter/twemoji
    {
        val twemojiSucks = mapOf(
            "#️⃣" to "#⃣",
            "*️⃣" to "*⃣",
            "0️⃣" to "0⃣",
            "1️⃣" to "1⃣",
            "2️⃣" to "2⃣",
            "3️⃣" to "3⃣",
            "4️⃣" to "4⃣",
            "5️⃣" to "5⃣",
            "6️⃣" to "6⃣",
            "7️⃣" to "7⃣",
            "8️⃣" to "8⃣",
            "9️⃣" to "9⃣",
            "\uD83C\uDD70️" to "\uD83C\uDD70"
        ).withDefault { it }

        val id = twemojiSucks.getValue(emoji).codePoints().toList()
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
        println(getEmojiURL("1️⃣"))
    }
}