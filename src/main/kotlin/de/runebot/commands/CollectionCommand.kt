package de.runebot.commands

import de.runebot.Util
import de.runebot.database.DB
import de.runebot.database.DBResponse
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

object CollectionCommand : RuneTextCommand
{
    private val add: RuneTextCommand.Subcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("add", "a"), Pair("add", "")),
        subcommands = listOf(
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("series", "se"), Pair("series <series name>", "Adds series to collection.")),
                { event, args, _ ->
                    event.message.author?.id?.value?.toLong()?.let {
                        if (DB.addToCollection(it, "All characters", args[0]) == DBResponse.SUCCESS)
                        {
                            Util.sendMessage(event, "Added series ${args[0]} to your collection.")
                        }
                    }
                },
                listOf()
            ),
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("character", "ch"), Pair("character <character name>", "Adds character to collection.")),
                { event, args, _ ->
                    event.message.author?.id?.value?.toLong()?.let {
                        if (DB.addToCollection(it, args[0], "None") == DBResponse.SUCCESS)
                        {
                            Util.sendMessage(event, "Added character ${args[0]} to your collection.")
                        }
                    }
                },
                listOf()
            ),
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("combo", "co"), Pair("combo <character name> <series name>", "Adds combo to collection.")),
                { event, args, _ ->
                    event.message.author?.id?.value?.toLong()?.let {
                        if (DB.addToCollection(it, args[0], args[1]) == DBResponse.SUCCESS)
                        {
                            Util.sendMessage(event, "Added character ${args[0]} from series ${args[1]} to your collection.")
                        }
                    }
                },
                listOf()
            )
        )
    )

    private val yeet: RuneTextCommand.Subcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("yeet", "y"), Pair("yeet", "")),
        subcommands = listOf(
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("series", "se"), Pair("series <series name>", "Removes series from collection.")),
                { event, args, _ ->
                    event.message.author?.id?.value?.toLong()?.let {
                        if (DB.removeFromCollection(it, "All characters", args[0]) == DBResponse.SUCCESS)
                        {
                            Util.sendMessage(event, "Removed series ${args[0]} from your collection.")
                        }
                    }
                },
                listOf()
            ),
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("character", "ch"), Pair("character <character name>", "Removes character from collection.")),
                { event, args, _ ->
                    event.message.author?.id?.value?.toLong()?.let {
                        if (DB.removeFromCollection(it, args[0], "None") == DBResponse.SUCCESS)
                        {
                            Util.sendMessage(event, "Removed character ${args[0]} from your collection.")
                        }
                    }
                },
                listOf()
            ),
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("combo", "co"), Pair("combo <character name> <series name>", "Removes combo from collection.")),
                { event, args, _ ->
                    event.message.author?.id?.value?.toLong()?.let {
                        if (DB.removeFromCollection(it, args[0], args[1]) == DBResponse.SUCCESS)
                        {
                            Util.sendMessage(event, "Removed character ${args[0]} from series ${args[1]} from your collection.")
                        }
                    }
                },
                listOf()
            )
        )
    )

    private val find: RuneTextCommand.Subcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("find", "f"), Pair("find", "")),
        subcommands = listOf(
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("series", "se"), Pair("series <series name>", "Tells, which users collect the specified series.")),
                { event, args, _ ->
                    val foundCollections: MutableMap<Long, MutableList<String>> = mutableMapOf()
                    DB.searchCollectionsBySeries(args[0]).forEach {
                        foundCollections.getOrPut(it.first) { mutableListOf() }.add(it.second)
                    }
                    val bobTheStringBuilder = StringBuilder("Following matches were found:${System.lineSeparator()}")
                    foundCollections.forEach { map ->
                        bobTheStringBuilder.append("\t> ${kord.getUser(Snowflake(map.key))?.username ?: "Unknown user"}${System.lineSeparator()}")
                        map.value.forEach {
                            bobTheStringBuilder.append("\t\t- $it${System.lineSeparator()}")
                        }
                    }
                    Util.sendMessage(event, bobTheStringBuilder.toString())
                },
                listOf()
            ),
            RuneTextCommand.Subcommand(
                RuneTextCommand.CommandDescription(listOf("character", "ch"), Pair("character <character name>", "Tells, which users collect the specified character.")),
                { event, args, _ ->
                    val foundCollections: MutableMap<Long, MutableList<String>> = mutableMapOf()
                    DB.searchCollectionsByCharacter(args[0]).forEach {
                        foundCollections.getOrPut(it.first) { mutableListOf() }.add(it.second)
                    }
                    val bobTheStringBuilder = StringBuilder("Following matches were found:${System.lineSeparator()}")
                    foundCollections.forEach { map ->
                        bobTheStringBuilder.append("\t> ${kord.getUser(Snowflake(map.key))?.username ?: "Unknown user"}${System.lineSeparator()}")
                        map.value.forEach {
                            bobTheStringBuilder.append("\t\t- $it${System.lineSeparator()}")
                        }
                    }
                    Util.sendMessage(event, bobTheStringBuilder.toString())
                },
                listOf()
            )
        )
    )

    private val show: RuneTextCommand.Subcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(listOf("show", "s"), Pair("show <mention>", "Shows the collection of specified user.")),
        { event, args, _ ->
            if (event.message.mentionedUsers.toList().isEmpty())
            {
                event.message.author?.let { author ->
                    val finalString = StringBuilder("Collection of ${author.username}:${System.lineSeparator()}")
                    finalString.append(DB.getAllFromCollection(author.id.value.toLong()).joinToString(System.lineSeparator()) {
                        "${it.first} from ${it.second}"
                    })
                    finalString.toString()
                }
            }
            Util.sendMessage(event, event.message.mentionedUsers.let { flow ->
                if (flow.toList().isEmpty() && event.message.author == null) "No valid users found."
                else if (flow.toList().isEmpty() && event.message.author != null) event.message.author?.let { user ->
                    val finalString = StringBuilder("Collection of ${user.username}:${System.lineSeparator()}")
                    finalString.append(DB.getAllFromCollection(user.id.value.toLong()).joinToString(System.lineSeparator()) {
                        "${it.first} from ${it.second}"
                    })
                    finalString.toString()
                } ?: "This should not happen! (CollectionCommand.kt, ln. 162)"
                else flow.map { user ->
                    val finalString = StringBuilder("Collection of ${user.username}:${System.lineSeparator()}")
                    finalString.append(DB.getAllFromCollection(user.id.value.toLong()).joinToString(System.lineSeparator()) {
                        "${it.first} from ${it.second}"
                    })
                    finalString.toString()
                }.toList().joinToString(System.lineSeparator())
            })
        },
        emptyList()
    )

    private val collection: RuneTextCommand.Subcommand = RuneTextCommand.Subcommand(
        RuneTextCommand.CommandDescription(names, Pair(shortHelpText, "")),
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
    private lateinit var kord: Kord

    override fun prepare(kord: Kord)
    {
        this.kord = kord
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