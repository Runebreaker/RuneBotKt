package de.runebot

import dev.kord.common.Color
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.NamedFile
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random

object Util
{
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
}