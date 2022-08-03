package de.runebot

import de.runebot.behaviors.Behavior
import de.runebot.commands.ConfigCommand
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

            val content = this.message.content

            // if message is a message command
            if (content.startsWith(MessageCommand.prefix))
            {
                val commandName = content.split(" ")[0].removePrefix(MessageCommand.prefix)
                messageCommands[commandName]?.let {
                    // Checks if message creator is admon
                    if (!it.needsAdmin || MessageCommand.isAdmin(this))
                    {
                        it.execute(this, content.split(" ").toTypedArray())
                    }
                }
            }

            // auto message behavior
            Behavior.runAll(content, this)
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
            TestCmd,
            ConfigCommand
        ).forEach { cmd ->
            cmd.names.forEach { name -> messageCommands[name] = cmd }
        }
    }
}