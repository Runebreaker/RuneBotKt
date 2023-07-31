package de.runebot.commands

import de.runebot.EmojiUtil
import de.runebot.Util
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import kotlin.streams.toList


object WhatCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("what")
    override val shortHelpText: String
        get() = "when people speak bottom"
    override val longHelpText: String
        get() = "`$commandExample index`: Use as reply to message with Emojis in it. `index` determines what Emoji should be used and defaults to 0. `index` is zero-indexed."

    private lateinit var kord: Kord

    override fun prepare(kord: Kord)
    {
        this.kord = kord
        EmojiUtil.update()
        println("Emojis loaded from https://www.unicode.org/Public/emoji/latest/emoji-test.txt")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        event.message.referencedMessage?.let { referencedMessage ->
            val content = referencedMessage.content
            val index = args.getOrNull(1)?.toIntOrNull() ?: 0

            // searches the message for emojis
            val foundEmojis = EmojiUtil.findEmojisToURLs(content, event.guildId ?: Snowflake(0), kord)
            if (foundEmojis.isEmpty())
            {
                Util.sendMessage(event, "No Emoji found!")
                return
            }

            // creates background, should never fail
            val background = withContext(Dispatchers.IO) {
                ImageIO.read(WhatCommand::class.java.getResourceAsStream("/IDSB.jpg"))
            }
            val graphics = background.createGraphics()

            // loads image of emoji
            val emoji = loadEmoji(foundEmojis[index] ?: emptyList())
                ?: Util.sendMessage(event, "No image found for this emoji!").run { return }

            // author avatar is skipped if no image is found
            referencedMessage.getAuthorAsMemberOrNull()?.avatar?.cdnUrl?.let { cdnUrl ->
                ImageIO.read(URL(cdnUrl.toUrl()))
            }?.let {
                val circleBuffer = BufferedImage(250, 250, BufferedImage.TYPE_INT_ARGB)
                val cropG = circleBuffer.createGraphics()
                cropG.clip(Ellipse2D.Double(0.0, 0.0, 250.0, 250.0))
                cropG.drawImage(it, 0, 0, 250, 250, null)
                graphics.drawImage(circleBuffer, 257 - 125, 289 - 125, 250, 250, null) // face
            }

            graphics.drawImage(emoji, 512 - 60, 275 - 60, 120, 120, null) // emoji in speech bubble
            graphics.drawImage(emoji, 243 - 17, 919 - 17, 33, 33, null) // emoji in response text
            Util.sendImage(event.message.channel, "what.jpg", background)
            return
        }
        // if anything else fails, it must be user-error
        Util.sendMessage(event, "`${HelpCommand.commandExample} ${names.firstOrNull()}`")
    }

    private fun loadEmoji(possibleURLs: List<String>): BufferedImage?
    {
        possibleURLs.forEach { url ->
            try
            {
                return ImageIO.read(URL(url))
            } catch (e: Exception)
            {
                // e.printStackTrace()
            }
        }

        return null
    }


    private fun unicodeEmojiToHex(emoji: String): List<String>
    {
        return emoji.codePoints().toList().map { it.toString(16) }
    }
}