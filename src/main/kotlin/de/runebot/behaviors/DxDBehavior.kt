package de.runebot.behaviors

import de.runebot.Util
import dev.kord.core.event.message.MessageCreateEvent
import info.debatty.java.stringsimilarity.Cosine
import java.io.BufferedReader
import java.io.InputStreamReader

object DxDBehavior : Behavior
{
    val lines: List<String>
    val precomputedLines: List<Map<String, Int>>
    val comparator = Cosine(3)

    init
    {
        DxDBehavior::class.java.getResourceAsStream("/HighSchoolDxD.txt").use { stream ->
            InputStreamReader(stream!!, Charsets.UTF_8).use { inputReader ->
                BufferedReader(inputReader).use { bufferedReader ->
                    lines = bufferedReader.lines().toList().filter { it.length > comparator.k }
                }
            }
        }

        precomputedLines = lines.map { comparator.getProfile(it.lowercase()) } // precompute for similarity
    }

    override suspend fun run(content: String, messageCreateEvent: MessageCreateEvent)
    {
        val inputProfile = comparator.getProfile(content.lowercase()) // precompute for similarity

        val hits = mutableListOf<Pair<String, Double>>()

        precomputedLines.forEachIndexed { index, profile ->
            val similarity = comparator.similarity(profile, inputProfile)
            if (similarity >= .5)
            {
                val nextLine = lines.getOrNull(index + 1) ?: return@forEachIndexed
                hits.add(nextLine to similarity)
            }
        }

        if (hits.isEmpty()) return
        if (hits.size == 1)
        {
            Util.sendMessage(messageCreateEvent, hits.first().first)
            return
        }

        hits.sortByDescending { it.second }

        // debug
        hits.forEach { println(it) }
        println()

        val result = mutableListOf<String>()
        result.addAll(hits.filter { hits.maxOf { it.second } == it.second }.map { it.first }) // add all max similarity
        hits.removeAll { it.first in result } // remove all lines that are already added
        result.addAll(hits.subList(0, 1 + hits.size / 2).map { it.first }) // add half of rest for a bit of randomness (add at least 1)

        Util.sendMessage(messageCreateEvent, result.randomOrNull() ?: return) // send random line
    }
}