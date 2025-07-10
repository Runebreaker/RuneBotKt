package de.runebot

import de.runebot.database.DB
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.ReactionEmoji
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant

object Taskmaster
{
    private var running = false

    private var timers = DB.getAllTimers()

    suspend fun run()
    {
        running = true
        while (running)
        {
            checkTimers()
            delay(1000)
        }
    }

    private suspend fun checkTimers()
    {
        RuneBot.kord?.let { kord ->
            timers.filter {
                it.targetTime <= System.currentTimeMillis()
            }.forEach { (targetTime, message, channelId, messageId) ->
                try
                {
                    val channel = MessageChannelBehavior(Snowflake(channelId), kord)
                    val channelMessage = channel.getMessage(Snowflake(messageId))
                    val builtMessage: StringBuilder = StringBuilder(message)
                    builtMessage.append(System.lineSeparator())
                    channelMessage.author?.let {
                        builtMessage.append(it.mention).append(" ")
                    }
                    channelMessage.getReactors(ReactionEmoji.Unicode(Emojis.alarmClock.unicode)).collect {
                        if (it.isBot || builtMessage.contains(it.mention)) return@collect
                        builtMessage.append(it.mention).append(" ")
                    }
                    Util.sendMessage(channel, builtMessage.toString())

                } catch (exception: Exception)
                {
                    exception.printStackTrace()
                    System.err.println("Timer $message at ${Instant.fromEpochMilliseconds(targetTime)} triggered with an exception and will be removed.")
                }

                DB.removeTimer(channelId, messageId)
                updateTimers()
            }
        }
    }

    fun updateTimers()
    {
        timers = DB.getAllTimers()
    }
}