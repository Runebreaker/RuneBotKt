@file:OptIn(KordPreview::class)

package de.runebot.commands

import de.runebot.Util
import de.runebot.Util.PuzzleType.Companion.toInfoString
import de.runebot.database.DB
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.GuildButtonInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.live.channel.live
import dev.kord.core.live.channel.onMessageCreate
import dev.kord.core.on
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

object PuzzleCommand : MessageCommandInterface
{
    var newPuzzle = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(
            listOf("new", "n"),
            Pair("new <type> <name> <description> <difficulty> <maxAttempts> <rewardType> <rewardAmount> <puzzleDetails>", "Create a new puzzle.")
        ),
        { event, args, _ ->
            event.message.author?.let {
                when (args.size)
                {
                    0 ->
                    {
                        event.message.channel.createMessage {
                            creationMenu.makeEmbed(this, creationActionRow)
                        }
                        return@let
                    }

                    else ->
                    {
                        Util.sendMessage(event, "Invalid arguments!")
                        return@let
                    }
                }
            }
        },
        emptyList()
    )

    private val puzzleActionRowBuilder = ActionRowBuilder().apply {
        interactionButton(
            style = ButtonStyle.Primary,
            customId = "confirm",
            builder = {
                emoji = DiscordPartialEmoji(name = ReactionEmoji.Unicode(Emojis.playPause.unicode).name)
            }
        )
    }

    var puzzle = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(
            listOf("puzzle", "puzzles", "pzl", "pzls"),
            Pair("puzzle", "Get a puzzle you haven't solved yet, or the currently active puzzle.")
        ),
        { event, args, _ ->
            event.message.author?.let {
                val activePuzzleId = DB.getActivePuzzleIdForUser(it.id.value.toLong())
                val puzzle = if (activePuzzleId == null) DB.getRandomPuzzle(it.id.value.toLong()) else DB.getPuzzle(activePuzzleId)
                puzzle?.let { puz ->
                    if (puzzleCatalogues[puz.puzzleId] == null)
                    {
                        puzzleCatalogues[puz.puzzleId] = Util.EmbedCatalogue()
                        puzzleCatalogues[puz.puzzleId]!!.catalogue.pages.add(puz.createInfoPage(it))
                        puzzleCatalogues[puz.puzzleId]!!.catalogue.pages.add(puz.createPuzzlePage(it))
                        kord.on<GuildButtonInteractionCreateEvent>(consumer = {
                            if (interaction.componentId != "confirm") return@on
                            if (activePuzzleId != puz.puzzleId) puz.onStart(it.id.value.toLong())
                            interaction.respondEphemeral {
                                puzzleCatalogues[puz.puzzleId]!!.catalogue.gotoPage(1)
                                puzzleCatalogues[puz.puzzleId]!!.makeEmbed(this, null)
                            }
                        })
                    }
                    event.message.channel.createMessage {
                        puzzleCatalogues[puz.puzzleId]!!.catalogue.gotoPage(0)
                        puzzleCatalogues[puz.puzzleId]!!.makeEmbed(this, puzzleActionRowBuilder)
                    }
                }
            }
                ?: Util.sendMessage(event, "You have solved all puzzles!")
        },
        listOf(newPuzzle)
    )

    override val names: List<String>
        get() = listOf("puzzle", "puzzles", "pzl", "pzls")
    override val shortHelpText: String
        get() = "Get a puzzle you haven't solved yet."
    override val longHelpText: String
        get() = puzzle.toTree().toString()
    private lateinit var kord: Kord

    private val puzzleCatalogues = mutableMapOf<Int, Util.EmbedCatalogue>()
    private val creationMenu = Util.createPuzzleCreationMenu()
    private val creationActionRow = Util.createPuzzleCreationMenuActionRow()

    override suspend fun prepare(kord: Kord)
    {
        this.kord = kord
        kord.on<GuildButtonInteractionCreateEvent>(consumer = { handleInteraction(interaction) })
        kord.createGlobalChatInputCommand("pause", "Pause the current puzzle.") { }
        kord.createGlobalChatInputCommand("hint", "Request a hint for your current puzzle.") { }
        kord.createGlobalChatInputCommand("solve", "Submit your solution for your current puzzle.") {
            string("solution", "The solution to the puzzle.") {
                required = true
            }
        }
        kord.on<ChatInputCommandInteractionCreateEvent> {
            when (interaction.command.rootName)
            {
                "solve" -> handleSolveAttempt(interaction)
                "pause" -> handlePause(interaction)
                "hint" -> handleHintRequest(interaction)
            }
        }
        println("Puzzle command prepared!")
    }

    private suspend fun handleHintRequest(interaction: ChatInputCommandInteraction)
    {
        val response = interaction.deferEphemeralResponse()
        val userId = interaction.user.id.value.toLong()
        val puzzle = DB.getActivePuzzleIdForUser(userId)?.let { DB.getPuzzle(it) }

        if (puzzle == null)
        {
            response.respond { content = "You don't have an active puzzle! Get one using >puzzle!" }
            return
        }
        response.respond {
            puzzle.createHintCatalogue().makeEmbed(this)
        }
    }

    private suspend fun handlePause(interaction: ChatInputCommandInteraction)
    {
        val response = interaction.deferEphemeralResponse()
        val userId = interaction.user.id.value.toLong()
        DB.getActivePuzzleIdForUser(userId)?.let { DB.getPuzzle(it) }?.onPause(userId)
        response.respond { content = "Puzzle paused! You can try your luck again using >puzzle!" }
    }

    private suspend fun handleSolveAttempt(interaction: ChatInputCommandInteraction)
    {
        val response = interaction.deferEphemeralResponse()
        val userId = interaction.user.id.value.toLong()
        val puzzle = DB.getActivePuzzleIdForUser(userId)?.let { DB.getPuzzle(it) }
        if (puzzle == null)
        {
            response.respond { content = "You don't have an active puzzle! Get one using >puzzle!" }
            return
        }
        val attemptsTaken = puzzle.let { DB.getPuzzleStatsForUser(userId, it.puzzleId)?.first }
        val submission = interaction.command.strings["solution"]!!
        val isCorrect = puzzle.checkSolution(submission)
        val correctString = "Congratulations! The answer '${submission}' is correct!"
        val rewardString = "You have been awarded ${puzzle.rewardAmount} ${puzzle.rewardType.id}!"
        val noRewardString = "Sadly, you have taken too many attempts and will not be rewarded. Better luck next time!"
        if (isCorrect) puzzle.onSolve(userId)
        else puzzle.onFail(userId)
        response.respond {
            content =
                if (isCorrect)
                {
                    if (attemptsTaken != null && attemptsTaken <= puzzle.maxAttempts) "$correctString $rewardString"
                    else "$correctString $noRewardString"
                }
                else "Try again! (Try #${DB.getPuzzleStatsForUser(userId, puzzle.puzzleId)?.first} of ${puzzle.maxAttempts})"
        }
    }

    @OptIn(KordPreview::class)
    private suspend fun handleInteraction(interaction: GuildButtonInteraction)
    {
        interaction.message.edit {
            attachments = mutableListOf()
            files.clear()
            if (interaction.componentId == "create")
            {
                interaction.user.let { user ->
                    user.getDmChannel().live().let { channel ->
                        channel.channel.createMessage("Hey there! Let's create a new puzzle! You can go back a step using ´>back´ at any time.")
                        channel.channel.createMessage(Util.Puzzle.TYPE_DIALOGUE)
                        var puzzleType: Util.PuzzleType? = null
                        val stateMachine = PuzzleCreationStateMachine()
                        val scope = CoroutineScope(SupervisorJob())
                        channel.onMessageCreate(scope) { messageCreateEvent ->
                            if (messageCreateEvent.message.author?.isBot != false) return@onMessageCreate
                            if (stateMachine.isBeginning() && messageCreateEvent.message.content.startsWith(">back")) puzzleType = null
                            if (puzzleType == null)
                            {
                                messageCreateEvent.message.content.toIntOrNull()?.let {
                                    puzzleType = Util.PuzzleType.fromId(it)
                                    messageCreateEvent.message.channel.createMessage(stateMachine.getDialogue())
                                } ?: messageCreateEvent.message.channel.createMessage("Please choose a puzzle type. (0: Input-Output, 1: Generator)")
                                return@onMessageCreate
                            }
                            if (stateMachine.isFinished())
                            {
                                val details = stateMachine.getDetails().copy()
                                stateMachine.reset()
                                DB.storePuzzle(
                                    user.id.value.toLong(),
                                    details.name,
                                    details.description,
                                    details.difficulty,
                                    details.maxAttempts,
                                    details.rewardType,
                                    details.rewardAmount,
                                    messageCreateEvent.message.content,
                                    puzzleType!!,
                                    details.hints
                                )
                                messageCreateEvent.message.channel.createMessage("Puzzle created successfully!")
                                scope.cancel("Puzzle creation finished.")
                                return@onMessageCreate
                            }
                            stateMachine.submit(messageCreateEvent.message.content)
                            if (stateMachine.isFinished()) messageCreateEvent.message.channel.createMessage(puzzleType!!.toInfoString())
                            else messageCreateEvent.message.channel.createMessage(stateMachine.getDialogue())
                            println(stateMachine.getState())
                        }
                    }
                    creationMenu.makeEmbed(this, creationActionRow)
                }
            }
        }
        interaction.respondPublic { }
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        event.message.author?.let { DB.initializeUserStatsIfNotExists(it.id.value.toLong()) }
        if (names.contains(args[0].substring(1))) puzzle.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    class PuzzleCreationStateMachine(private var state: Int = 0, private val details: Util.PuzzleGenericInfo = Util.PuzzleGenericInfo(), private val stateCount: Int = 7)
    {
        fun nextState()
        {
            if (state < stateCount) state++
        }

        fun previousState()
        {
            if (state > 0) state--
        }

        fun reset()
        {
            state = 0
            details.reset()
        }

        fun getState() = state

        fun setState(state: Int)
        {
            this.state = state
        }

        fun isBeginning() = state == 0

        fun isFinished() = state == stateCount

        fun submit(value: String)
        {
            if (value.startsWith(">back"))
            {
                previousState()
                return
            }
            when (state)
            {
                0 -> details.name = value
                1 -> details.description = value
                2 -> details.difficulty = value.toInt()
                3 -> details.maxAttempts = value.toInt()
                4 ->
                {
                    details.rewardType = Util.PuzzleRewardType.fromId(value)
                    if (details.rewardType == Util.PuzzleRewardType.BADGE) nextState()
                }

                5 -> details.rewardAmount = value.toInt()
                6 -> details.hints = value.split(",").toMutableList()
            }

            nextState()
        }

        private val dialogues = mapOf(
            0 to "What is the name of the puzzle?",
            1 to "What is the description of the puzzle?",
            2 to "How difficult is the puzzle on a scale of 1(easy)-5(hard)?",
            3 to "How many attempts does the user have to solve the puzzle?",
            4 to "What is the reward type? (0: Puzzle Points, 1: Badge)",
            5 to "What is the reward amount?",
            6 to "Give any hints you want people to be able to access gradually. (Optional, separated by commas.)"
        )

        fun getDialogue(): String = dialogues[state] ?: "Unexpected value for state! Please contact the developer."

        fun getDetails() = details
    }
}