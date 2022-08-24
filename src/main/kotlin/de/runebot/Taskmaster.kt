package de.runebot

import kotlinx.coroutines.delay

object Taskmaster
{
    var running = false

    suspend fun run()
    {
        running = true
        while (running)
        {


            delay(1000)
        }
    }

    suspend fun checkTimers()
    {
        RuneBot.kord?.let { }
    }
}