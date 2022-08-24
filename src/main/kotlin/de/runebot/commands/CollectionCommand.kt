package de.runebot.commands

import de.runebot.Util
import de.runebot.database.DB
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent

object CollectionCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("collection", "col", "c")
    override val shortHelpText: String
        get() = "collection functionality for use with mudae"
    override val longHelpText: String
        get()
        {
            return Util.StringTree(commandExample).getRoot()
                .addChild("add")
                .addChild("series <series name>                 - Adds series to collection.").moveUp()
                .addChild("character <character name>           - Adds character to collection.").moveUp()
                .addChild("combo <character name> <series name> - Adds combo to collection.").moveUp()
                .moveUp()
                .addChild("yeet")
                .addChild("series <series name>                 - Removes series from collection.").moveUp()
                .addChild("character <character name>           - Removes character from collection.").moveUp()
                .addChild("combo <character name> <series name> - Removes combo from collection.").moveUp()
                .moveUp()
                .addChild("find")
                .addChild("series <series name>                 - Tells, which users collect the specified series.").moveUp()
                .addChild("character <character name>           - Tells, which users collect the specified character.").moveUp()
                .addChild("combo <character name> <series name> - Tells, which users collect the specified combo.").moveUp()
                .moveUp()
                .addChild("show <mention>                        - Shows the collection of specified user.")
                .moveToRoot()
                .toString()
        }

    override fun prepare(kord: Kord)
    {
        println("Collection command ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event, "Try >(`${names.joinToString("` | `")}`) `help`")
            return
        }

        event.message.author?.let {
            when (args[1])
            {
                "add" ->
                {
                    when (args[2])
                    {
                        "series" ->
                        {
                            DB.addToCollection(it.id.value.toLong(), "All characters", args[2])
                        }
                        "character" ->
                        {

                        }
                        "combo" ->
                        {

                        }
                        else ->
                        {

                        }
                    }
                }
                "yeet" ->
                {

                }
                "find" ->
                {

                }
                "show" ->
                {

                }
                else ->
                {
                    Util.sendMessage(event, "Try >(`${names.joinToString("` | `")}`) `help`")
                }
            }
        }
    }
}