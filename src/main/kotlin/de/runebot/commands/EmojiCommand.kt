package de.runebot.commands

import de.runebot.EmojiUtil.findEmojis
import de.runebot.EmojiUtil.getAlternativesFor
import de.runebot.EmojiUtil.getEmojipediaURL
import de.runebot.EmojipediaStyle
import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object EmojiCommand : MessageCommandInterface
{
    private lateinit var kord: Kord
    override val names: List<String>
        get() = listOf("emoji")
    override val shortHelpText: String
        get() = "returns images for emojis"
    override val longHelpText: String
        get() = "`$commandExample emoji style`: returns emoji image for given input in given style (random if not provided)\n" +
                "available styles are: `twitter`, `apple`, `google`, `samsung`, `whatsapp`, `microsoft`, `twitter_new`, `teams`, `openmoji`, `facebook`, `emojipedia`, `joypixels`, `toss_face` and `noto_font`"

    override fun prepare(kord: Kord)
    {
        this.kord = kord
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val inputEmoji = args.getOrNull(1) ?: Util.sendMessage(event, "no input provided").run { return }
        val style = try
        {
            EmojipediaStyle.valueOf(args.getOrNull(2)?.uppercase() ?: EmojipediaStyle.values().random().toString())
        } catch (_: IllegalArgumentException)
        {
            EmojipediaStyle.values().random()
        }

        println(args.getOrNull(2))

        Util.sendMessage(event,
            findEmojis(inputEmoji)
                .flatMap { getAlternativesFor(it.second) }
                .map { getEmojipediaURL(it, style) }
                .joinToString(separator = "\n") { it }
        )
    }
}