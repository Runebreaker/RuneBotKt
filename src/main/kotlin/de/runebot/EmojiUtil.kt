package de.runebot

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.EmojiData
import dev.kord.core.entity.GuildEmoji
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.days

object EmojiUtil
{
    private var emojis = mutableListOf<Emoji>()
    private var lastEmojiUpdate: Long = 0

    private val guildEmojiRegex = Regex("<a?:[a-zA-Z0-9_]+:[0-9]+>")

    fun update()
    {
        if (System.currentTimeMillis() > lastEmojiUpdate + 1.days.inWholeMilliseconds)
        {
            loadCurrentEmojiList()
        }
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


                    val codePoints = it.substringBefore(";").trim().split(" ")
                        .map {
                            try
                            {
                                it.toInt(16)
                            } catch (_: NumberFormatException)
                            {
                                println("Error with $it")
                                0
                            }
                        }
                    Emoji(
                        codePoints = codePoints,
                        status = try
                        {
                            EmojiStatus.valueOf(it.substringAfter(";").trim().substringBefore(" ").replace('-', '_').uppercase())
                        } catch (_: IllegalArgumentException)
                        {
                            EmojiStatus.ERROR
                        },
                        emoji = codePoints.joinToString(separator = "") { Character.toChars(it).joinToString("") },
                        version = "E" + it.substringAfter("#").substringAfter("E").substringBefore(" "),
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

    /**
     * find emojis
     * @param text where to search
     * @return list of all findings, index in input to emoji data
     */
    fun findEmojis(text: String): List<Pair<Int, Emoji>>
    {
        update()

        val result = mutableListOf<Pair<Int, Emoji>>()

        emojis.forEach {
            var startIndex = 0
            var indexToEmoji: Pair<Int, String> = 0 to "🍆" // just a default, should never be used
            while (text.findAnyOf(listOf(it.emoji), startIndex = startIndex)?.also { findResult -> indexToEmoji = findResult } != null)
            {
                val (i, emoji) = indexToEmoji
                startIndex = i + emoji.length
                result.add(i to it)
            }
        }

        result.toList().forEach { (index, emoji) ->
            result.removeAll { it.first in (index + 1 until index + emoji.emoji.length) } // deletes overlapping emojis (e.g. 🇩🇪)
            result.removeAll { it.first == index && it.second.codePoints.size < emoji.codePoints.size } // deletes shorter versions of the same emoji
            result.removeAll { it.second.name == emoji.name && it.second.codePoints.size < emoji.codePoints.size } // deletes shorter versions of the same emoji
        }

        return result
    }

    fun getEmojiForString(emoji: String): Emoji?
    {
        return emojis.find { it.emoji == emoji }
    }

    fun getAlternativesFor(emoji: String): List<Emoji>
    {
        return getAlternativesFor(getEmojiForString(emoji) ?: return emptyList())
    }

    fun getAlternativesFor(emoji: Emoji): List<Emoji>
    {
        update()
        return emojis.filter { it.name == emoji.name }
    }

    fun findGuildEmojis(text: String): List<Pair<Int, Snowflake>>
    {
        return guildEmojiRegex.findAll(text).mapNotNull { matchResult ->
            matchResult.value.split(":")[2].removeSuffix(">").toLongOrNull()?.let { id ->
                matchResult.range.first to Snowflake(id)
            }
        }.toList()
    }

    /**
     * @return list of order index to URL
     */
    fun findEmojisToURLs(text: String, guildId: Snowflake, kord: Kord): Map<Int, List<String>>
    {
        val result = findEmojis(text).map { it.first to it.second }.toMutableList()

        result.toList().forEach { (index, emoji) ->
            getAlternativesFor(emoji).forEach { result.add(index to it) }
        }

        val stringIndexToURLs = result.map { it.first to getTwemojiURL(it.second) }.toMutableList()

        stringIndexToURLs.addAll(findGuildEmojis(text).map { (index, snowflake) -> index to getGuildEmojiURL(snowflake, guildId, kord) })

        val toBeReturned = mutableMapOf<Int, List<String>>()
        stringIndexToURLs.groupBy({ it.first }, { it.second }).toList().sortedBy { it.first }.forEachIndexed { index, (key, value) -> toBeReturned[index] = value }
        return toBeReturned
    }

    fun getNewTwitterEmojiURL(emoji: Emoji): String
    {
        val id = emoji.codePoints.joinToString(separator = "-") { it.toString(16) }
        return "https://em-content.zobj.net/thumbs/160/twitter/348/${emoji.name.replace(' ', '-')}_$id.png"
    }

    fun getTwemojiURL(emoji: Emoji): String
    {
        val id = emoji.codePoints.joinToString(separator = "-") { it.toString(16) }
        return "https://cdn.jsdelivr.net/gh/twitter/twemoji@latest/assets/72x72/$id.png"
    }

    fun getEmojipediaURL(emoji: Emoji, style: EmojipediaStyle): String
    {
        val id = emoji.codePoints.joinToString(separator = "-") { it.toString(16) }
        return "${style.urlPrefix}${emoji.name.replace(' ', '-')}_$id.png"
    }

    fun getGuildEmojiURL(guildEmojiId: Snowflake, guildId: Snowflake, kord: Kord): String
    {
        return GuildEmoji(EmojiData(guildEmojiId, guildId), kord).image.cdnUrl.toUrl()
    }
}

/**
 * @param emoji this string will be rendered as an emoji by most systems
 */
data class Emoji(val codePoints: List<Int>, val status: EmojiStatus, val emoji: String, val version: String, val name: String)

enum class EmojiStatus
{
    COMPONENT, FULLY_QUALIFIED, MINIMALLY_QUALIFIED, UNQUALIFIED, ERROR
}

enum class EmojipediaStyle(val urlPrefix: String)
{
    TWITTER("https://em-content.zobj.net/thumbs/120/twitter/351/"),
    APPLE("https://em-content.zobj.net/thumbs/120/apple/354/"),
    GOOGLE("https://em-content.zobj.net/thumbs/120/google/350/"),
    GOOGLE_ANIMATED("https://em-content.zobj.net/source/animated-noto-color-emoji/356/"),
    SAMSUNG("https://em-content.zobj.net/thumbs/120/samsung/349/"),
    WHATSAPP("https://em-content.zobj.net/thumbs/120/whatsapp/352/"),
    TELEGRAM("https://em-content.zobj.net/source/telegram/358/"),
    MICROSOFT("https://em-content.zobj.net/thumbs/120/microsoft/319/"),
    TWITTER_NEW("https://em-content.zobj.net/thumbs/160/twitter/348/"),
    TEAMS("https://em-content.zobj.net/source/microsoft-teams/363/"),
    SKYPE("https://em-content.zobj.net/source/skype/289/"),
    OPENMOJI("https://em-content.zobj.net/thumbs/120/openmoji/338/"),
    FACEBOOK("https://em-content.zobj.net/thumbs/120/facebook/355/"),
    MESSENGER("https://em-content.zobj.net/thumbs/240/facebook/65/"),
    EMOJIPEDIA("https://em-content.zobj.net/thumbs/120/emojipedia/294/"),
    JOYPIXELS("https://em-content.zobj.net/thumbs/120/joypixels/340/"),
    JOYMIXELS_ANIMATED("https://em-content.zobj.net/source/joypixels-animations/366/"),
    TOSS_FACE("https://em-content.zobj.net/thumbs/120/toss-face/372/"),
    NOTO_FONT("https://em-content.zobj.net/thumbs/120/noto-emoji/343/"),
    LG("https://em-content.zobj.net/thumbs/240/lg/307/"),
    HTC("https://em-content.zobj.net/thumbs/240/htc/122/"),
    MOZILLA("https://em-content.zobj.net/thumbs/240/mozilla/36/"),
    SOFTBANK("https://em-content.zobj.net/thumbs/240/softbank/145/"),
    DOCOMO("https://em-content.zobj.net/thumbs/240/docomo/205/"),
    AU_BY_KDDI("https://em-content.zobj.net/thumbs/240/au-kddi/190/"),
    EMOJIDEX("https://em-content.zobj.net/thumbs/240/emojidex/112/"),
}