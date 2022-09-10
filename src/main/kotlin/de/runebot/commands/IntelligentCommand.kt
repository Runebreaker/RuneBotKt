package de.runebot.commands

object IntelligentCommand : GifResponseCommand
{
    override val names: List<String>
        get() = listOf("intelligent")
    override val response: String
        get() = "https://tenor.com/view/buzz-lightyear-no-sign-of-intelligent-life-dumb-toy-story-gif-11489315"

    // TODO: Convert this to tag
}