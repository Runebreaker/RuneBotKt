package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.interaction.GlobalChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import java.io.BufferedReader
import java.io.InputStreamReader

object AcronymCommand : RuneTextCommand, RuneSlashCommand
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

    override suspend fun prepare(kord: Kord)
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
            createResponse(input)
        )
    }

    private fun createResponse(input: String): String
    {
        return input
            .split(System.lineSeparator()).joinToString(separator = System.lineSeparator() + System.lineSeparator()) {
                it.split(" ").joinToString(separator = System.lineSeparator()) { str ->
                    str.map { c ->
                        dictionary[c.lowercaseChar()]?.random() ?: c
                    }.joinToString(separator = " ")
                }
            }
    }

    override val name: String
        get() = "acronym"
    override val helpText: String
        get() = shortHelpText

    override suspend fun createCommand(builder: GlobalChatInputCreateBuilder)
    {
        with(builder)
        {
            string("word", "some alleged acronym") {
                required = true
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val response = interaction.deferPublicResponse()
            val input = interaction.command.strings["word"]!!
            response.respond { content = createResponse(input) }
        }
    }
}