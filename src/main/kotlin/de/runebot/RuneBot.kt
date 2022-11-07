package de.runebot

import dev.kord.common.entity.Snowflake
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
    val adminRoleSnowflake = Snowflake(System.getenv()["ADMIN_ROLE_ID"]?.toULong() ?: error("error reading admin role id"))
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

        println("Starting RuneBot with admin role id $adminRoleSnowflake...")

        kord?.let { kord ->
            Registry.messageCommands.forEach { it.prepare(kord) }

            kord.on<MessageCreateEvent> {
                // return if author is a bot or undefined
                if (message.author?.isBot != false) return@on

                if (!Registry.handleMessageCommands(this)) Registry.handleBehaviors(this)
            }

            kord.login {
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        }
    }
}