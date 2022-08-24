package de.runebot.commands

interface GifResponseCommand : StringResponseCommand
{
    override val shortHelpText: String
        get() = "sends dank meme gif"
    override val longHelpText: String
        get() = "`$commandExample`: sends gif"
}