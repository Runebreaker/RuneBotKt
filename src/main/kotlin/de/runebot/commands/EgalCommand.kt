package de.runebot.commands

object EgalCommand : GifResponseCommand
{
    override val names: List<String>
        get() = listOf("egal")
    override val response: String
        get() = "https://giphy.com/gifs/vol2cat-oliver-egal-wendler-ZG5KTqutRAfZ6i5OVR"
}