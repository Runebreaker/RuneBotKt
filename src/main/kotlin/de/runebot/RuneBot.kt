package de.runebot

import de.runebot.commands.MessageCommand
import de.runebot.commands.TestCmd
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking

object RuneBot
{
    val messageCommands = mutableMapOf<String, MessageCommand>()

    val token = System.getenv()["BOT_TOKEN"] ?: error("error reading bot token")

    @JvmStatic
    fun main(args: Array<String>)
    {
        registerMessageCommands()
        runBlocking { bot() }
    }

    private suspend fun bot()
    {
        val kord = Kord(token)

        messageCommands.values.forEach { it.prepare(kord) }

        kord.on<MessageCreateEvent> {
            // return if author is a bot or undefined
            if (message.author?.isBot != false) return@on

            val messageContent = this.message.content

            // if message is a message command
            if (messageContent.startsWith(MessageCommand.prefix))
            {
                val commandName = messageContent.split(" ")[0].removePrefix(MessageCommand.prefix)
                messageCommands[commandName]?.execute(this, messageContent.split(" ").toTypedArray())
            }
        }

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

    private fun registerMessageCommands()
    {
        listOf<MessageCommand>(
            // add your MessageCommands here
            TestCmd
        ).forEach {
            messageCommands[it.name] = it
        }
    }
}