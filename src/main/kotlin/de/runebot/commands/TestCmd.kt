package de.runebot.commands

import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.random.Random

object TestCmd : MessageCommand
{
    override val names: List<String>
        get() = listOf("test")

    override fun prepare(kord: Kord)
    {
        println("test command prepared")
    }

    override suspend fun execute(event: MessageCreateEvent, args: Array<String>)
    {
        Config.store(Random.nextInt().toString(), Random.nextInt().toString())
    }
}