package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlin.random.Random

object MockCommand : MessageCommandInterface
{
    override val names: List<String>
        get() = listOf("mock")
    override val shortHelpText: String
        get() = "accurately imitate someone's message"
    override val longHelpText: String
        get() = "`$commandExample`: use this command as a reply to a message you want to mock"

    override fun prepare(kord: Kord)
    {

    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        event.message.referencedMessage?.let { referencedMessage ->
            Util.sendMessage(event, referencedMessage.content.mockify())
        } ?: Util.sendMessage(event, "`>help mock`") //TODO: mock last message
    }

    fun String.mockify(): String
    {
        val sb = StringBuilder()
        var toggle = Random.nextBoolean()
        this.forEach {
            sb.append(if (toggle) it.uppercase() else it.lowercase())
            if (Random.nextDouble() < .8) toggle = !toggle
        }
        return sb.toString()
    }
}