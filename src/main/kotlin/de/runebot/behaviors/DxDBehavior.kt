package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent
import info.debatty.java.stringsimilarity.JaroWinkler
import java.io.BufferedReader
import java.io.InputStreamReader

object DxDBehavior : Behavior
{
    val lines: List<String>
    val comparator = JaroWinkler()

    init
    {
        DxDBehavior::class.java.getResourceAsStream("/HighSchoolDxD.txt").use { stream ->
            InputStreamReader(stream!!, Charsets.UTF_8).use { inputReader ->
                BufferedReader(inputReader).use { bufferedReader ->
                    lines = bufferedReader.lines().toList().filter { it.length > 3 }
                }
            }
        }
    }

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        val results = mutableListOf<String>()

        lines.forEachIndexed { index, s ->
            if (comparator.similarity(s.lowercase(), content.lowercase()) > .8)
            {
                //println("$s: ${comparator.similarity(s.lowercase(), content.lowercase())}")
                results.add(lines.getOrNull(index + 1) ?: return@forEachIndexed)
            }
        }

        Util.sendMessage(messageCreateEvent, results.randomOrNull() ?: return)
    }
}