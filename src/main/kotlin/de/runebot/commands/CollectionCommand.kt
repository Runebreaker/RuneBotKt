package de.runebot.commands

import de.runebot.Util
import de.runebot.database.DB
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

object CollectionCommand : MessageCommandInterface
{
    private val add: MessageCommandInterface.Subcommand = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("add", "a"), Pair("add", "")),
        subcommands = listOf(
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("series", "se"), Pair("series <series name>", "Adds series to collection.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.addToCollection(it, "All characters", args[0]) } },
                listOf()
            ),
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("character", "ch"), Pair("character <character name>", "Adds character to collection.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.addToCollection(it, args[0], "None") } },
                listOf()
            ),
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("combo", "co"), Pair("combo <character name> <series name>", "Adds combo to collection.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.addToCollection(it, args[0], args[1]) } },
                listOf()
            )
        )
    )

    private val yeet: MessageCommandInterface.Subcommand = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("yeet", "y"), Pair("yeet", "")),
        subcommands = listOf(
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("series", "se"), Pair("series <series name>", "Removes series from collection.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.removeFromCollection(it, "All characters", args[0]) } },
                listOf()
            ),
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("character", "ch"), Pair("character <character name>", "Removes character from collection.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.removeFromCollection(it, args[0], "None") } },
                listOf()
            ),
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("combo", "co"), Pair("combo <character name> <series name>", "Removes combo from collection.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.removeFromCollection(it, args[0], args[1]) } },
                listOf()
            )
        )
    )

    private val find: MessageCommandInterface.Subcommand = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("find", "f"), Pair("find", "")),
        subcommands = listOf(
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("series", "se"), Pair("series <series name>", "Tells, which users collect the specified series.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.searchCollectionsBySeries(args[0]) } },
                listOf()
            ),
            MessageCommandInterface.Subcommand(
                MessageCommandInterface.CommandDescription(listOf("character", "ch"), Pair("character <character name>", "Tells, which users collect the specified character.")),
                { event, args, _ -> event.message.author?.id?.value?.toLong()?.let { DB.searchCollectionsByCharacter(args[0]) } },
                listOf()
            )
        )
    )

    private val show: MessageCommandInterface.Subcommand = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("show", "s"), Pair("show <mention>", "Shows the collection of specified user.")),
        { event, args, _ ->
            Util.sendMessage(event, event.message.mentionedUsers.map { user ->
                val finalString = StringBuilder("Collection of ${user.username}:${System.lineSeparator()}")
                finalString.append(DB.getAllFromCollection(user.id.value.toLong()).joinToString(System.lineSeparator()) {
                    "${it.first} from ${it.second}"
                })
                finalString.toString()
            }.toList().joinToString(System.lineSeparator()))
        },
        emptyList()
    )

    private val collection: MessageCommandInterface.Subcommand = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(names, Pair(shortHelpText, "")),
        subcommands = listOf(
            add,
            yeet,
            find,
            show
        )
    )

    override val names: List<String>
        get() = listOf("collection", "col", "c")
    override val shortHelpText: String
        get() = "collection functionality for use with mudae"
    override val longHelpText: String
        get() = collection.toTree().toString()

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        if (args.size <= 1)
        {
            Util.sendMessage(event, "Try >(`${names.joinToString("` | `")}`) `help`")
            return
        }

        if (names.contains(args[0].substring(1))) collection.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }
}