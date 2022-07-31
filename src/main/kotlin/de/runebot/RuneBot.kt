package de.runebot

import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking

object RuneBot
{
    val token = "OTMwNDIzOTQyNDE2NTgwNjA4.GkyxYt.b09k5M2HyQv9cMBRNv4LW3ScsijGyw0Dyv90DI"

    @JvmStatic
    fun main(args: Array<String>)
    {
        runBlocking { bot() }
    }

    private suspend fun bot()
    {
        val kord = Kord(token)

        kord.on<MessageCreateEvent> {
            println(this.message.content)
        }

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }
}