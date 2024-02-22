package de.runebot

import de.runebot.database.DB
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.ReactionEmoji
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import java.time.format.DateTimeFormatter

object Taskmaster
{
    private var running = false

    private var oldTimers = DB.getAllOldTimers()
    private var timers = DB.getAllTimers()

    suspend fun run()
    {
        running = true
        while (running)
        {
            checkTimers()
            checkOldTimers()
            delay(1000)
        }
    }

    private suspend fun checkOldTimers() // TODO: support new subscription system: make new DB and ignore old one
    {
        RuneBot.kord?.let { kord ->
            oldTimers.filter {
                it.targetTime <= System.currentTimeMillis()
            }.forEach { (_, message, channelId, messageId) ->
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
                DB.removeOldTimer(channelId, messageId)
                updateTimers()
            }
        }
    }

    private suspend fun checkTimers()
    {
        RuneBot.kord?.let { kord ->
            val now = Clock.System.now()

            timers
                .sortedBy { it.targetTime } // sort for performance improvement
                .forEach { (creatorId, targetTime, message, subscriberIds) ->
                    if (targetTime > now)
                    {
                        return // possible, because list is sorted
                    }

                    subscriberIds.forEach { userId ->
                        UserBehavior(Snowflake(userId), kord).getDmChannelOrNull()?.let { dmChannel ->
                            dmChannel.createMessage {
                                content = "**Reminder for ${DateTimeFormatter.RFC_1123_DATE_TIME}**" +
                                        "\n$message"
                            }
                        }
                    }

                    // TODO: remove from db
                }
        }
    }

    fun updateTimers()
    {
        oldTimers = DB.getAllOldTimers()
    }
}