package de.runebot.commands

object BonkCommand : GifResponseCommand
{
    override val names: List<String>
        get() = listOf("bonk")
    override val response: String
        get() = "https://media1.tenor.com/images/ae34b2d6cbac150bfddf05133a0d8337/tenor.gif?itemid=14889944"

    // TODO: Convert this to tag
}