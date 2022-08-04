package de.runebot

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking

object RuneBot
{
    val token = System.getenv()["BOT_TOKEN"] ?: error("error reading bot token")

    @JvmStatic
    fun main(args: Array<String>)
    {
        runBlocking { bot() }
    }

    private suspend fun bot()
    {
        val kord = Kord(token)

        Registry.messageCommands.forEach { it.prepare(kord) }

        kord.on<MessageCreateEvent> {
            // return if author is a bot or undefined
            if (message.author?.isBot != false) return@on

            Registry.handleMessageCommands(this)
            Registry.handleBehaviors(this)
        }

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }
}