package de.runebot

import de.runebot.config.Config
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object RuneBot
{
    val scope: CoroutineScope = CoroutineScope(SupervisorJob())

    val token = System.getenv()["BOT_TOKEN"] ?: error("error reading bot token")
    var kord: Kord? = null
        private set

    @JvmStatic
    fun main(args: Array<String>)
    {
        runBlocking {
            launch { bot() }
            launch { Taskmaster.run() }
        }
    }

    private suspend fun bot()
    {
        kord = Kord(token)

        kord?.let { kord ->
            Registry.messageCommands.forEach { it.prepare(kord) }

            kord.on<MessageCreateEvent> {
                if (Config.getValue(this.guildId?.value ?: return@on, "interactsWithBots") == "true")
                {
                    // return if author is self or undefined
                    if (message.author?.id == kord.selfId) return@on
                }
                else
                {
                    // return if author is a bot or undefined
                    if (message.author?.isBot != false) return@on
                }

                if (!Registry.handleMessageCommands(this)) Registry.handleBehaviors(this)
            }

            kord.login {
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        }
    }
}