package de.runebot

import kotlin.random.Random

object Util
{
    fun String.randomizeCapitalization(): String
    {
        val stringBuilder = StringBuilder()
        this.forEach { stringBuilder.append(if (Random.nextBoolean()) it.uppercase() else it.lowercase()) }
        return stringBuilder.toString()
    }
}