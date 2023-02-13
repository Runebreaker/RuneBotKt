package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import java.io.BufferedReader
import java.io.InputStreamReader

object AcronymCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("acronym", "acro")
    override val shortHelpText: String
        get() = "generates acronym interpretations"
    override val longHelpText: String
        get() = "`$commandExample input` or reply to message: generates acronym interpretation for input String."

    private lateinit var kord: Kord

    private val dictionary: Map<Char, List<String>>

    init
    {
        val buffer = mutableMapOf<Char, MutableList<String>>()
        AcronymCommand::class.java.getResourceAsStream("/words.txt").use { stream ->
            InputStreamReader(stream!!, Charsets.UTF_8).use { inputReader ->
                BufferedReader(inputReader).use { bufferedReader ->
                    bufferedReader.lines().filter { it.isNotBlank() }.forEach { str -> buffer.getOrPut(str[0].lowercaseChar()) { mutableListOf() }.add(str) }
                }
            }
        }
        dictionary = buffer
    }

    override fun prepare(kord: Kord)
    {
        this.kord = kord
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        val input =
            if (args.size > 1) args.drop(1).joinToString(separator = " ")
            else event.message.referencedMessage?.content
                ?: Util.sendMessage(event, "`${HelpCommand.commandExample} acronym`").run { return }

        Util.sendMessage(
            event,
            input
                .split(System.lineSeparator()).joinToString(separator = System.lineSeparator() + System.lineSeparator()) {
                    it.split(" ").joinToString(separator = System.lineSeparator()) { str ->
                        str.map { c ->
                            dictionary[c.lowercaseChar()]?.random() ?: c
                        }.joinToString(separator = " ")
                    }
                }
        )
    }
}