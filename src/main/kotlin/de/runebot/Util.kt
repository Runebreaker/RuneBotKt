package de.runebot

import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.NamedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
}