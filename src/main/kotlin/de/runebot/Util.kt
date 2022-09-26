package de.runebot

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.exception.RequestException
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
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
        sendImage(channel, path.fileName.toString(), withContext(Dispatchers.IO) {
            Files.newInputStream(path)
        })
    }

    suspend fun sendImage(channel: MessageChannelBehavior, fileName: String, inputStream: InputStream)
    {
        channel.createMessage { this.files.add(NamedFile(fileName, inputStream)) }
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


    // bullshittery begins here
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

        event.message.channel.createMessage { embeds.add(someEmbed) }
    }

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
}