package de.runebot.config

import de.runebot.Util.Rule
import de.runebot.commands.BehaviorCommand
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

object Config
{
    private val serializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var botConfig: BotConfigStructure = BotConfigStructure()
    private val guildConfigs: MutableMap<ULong, GuildConfigStructure> = mutableMapOf()
    private val pathToConfigDir = Path("config/")
    private val pathToBotConfig = pathToConfigDir.resolve("general.json")

    val applicationCommands
        get() = botConfig.applicationCommands

    init
    {
        if (Files.notExists(pathToConfigDir))
        {
            Files.createDirectory(pathToConfigDir)
        }

        if (Files.notExists(pathToBotConfig))
        {
            Files.createFile(pathToBotConfig)
            storeBotConfig()
        }

        pathToConfigDir.listDirectoryEntries().forEach { path ->
            if (path.toFile().name == "general.json")
            {
                botConfig = serializer.decodeFromString(path.toFile().readText())
            }
            else
            {
                guildConfigs[path.name.removeSuffix(".json").toULong()] = serializer.decodeFromString(path.toFile().readText())
            }
        }
    }

    fun addCommand(internalId: String, snowflake: Snowflake)
    {
        this.botConfig.applicationCommands[internalId] = snowflake
        storeBotConfig()
    }

    fun storeBotConfig()
    {
        if (Files.notExists(pathToBotConfig))
        {
            Files.createFile(pathToBotConfig)
        }

        Files.writeString(pathToBotConfig, serializer.encodeToString(botConfig))
    }

    fun setAdminRoleId(guild: ULong, adminRoleId: ULong)
    {
        getConfigForGuild(guild).adminRoleId = adminRoleId
        storeConfigForGuild(guild)
    }

    fun getAdminRoleId(guild: ULong): ULong?
    {
        return getConfigForGuild(guild).adminRoleId
    }

    fun getConfigForGuild(id: ULong): GuildConfigStructure
    {
        return guildConfigs.getOrPut(id) { GuildConfigStructure() }
    }

    fun storeConfigForGuild(id: ULong)
    {
        val pathToGuildConfig = pathToConfigDir.resolve("$id.json")
        if (Files.notExists(pathToGuildConfig))
        {
            Files.createFile(pathToGuildConfig)
        }

        Files.writeString(pathToGuildConfig, serializer.encodeToString(getConfigForGuild(id)))
    }

    //region KeyValueStorage

    fun storeValue(guild: ULong, key: String, value: String)
    {
        getConfigForGuild(guild).keyValueStorage[key] = value
        storeConfigForGuild(guild)
    }

    fun resetValue(guild: ULong, key: String)
    {
        getConfigForGuild(guild).keyValueStorage.remove(key)
        storeConfigForGuild(guild)
    }

    fun getValue(guild: ULong, key: String): String?
    {
        return getConfigForGuild(guild).keyValueStorage[key]
    }

    //endregion

    //region UwURules

    fun storeRule(guild: ULong, key: String, value: String)
    {
        getConfigForGuild(guild).uwurules[key] = value
        storeConfigForGuild(guild)
    }

    fun resetRule(guild: ULong, key: String)
    {
        getConfigForGuild(guild).uwurules.remove(key)
        storeConfigForGuild(guild)
    }

    fun getRules(guild: ULong): List<Rule>
    {
        return getConfigForGuild(guild).uwurules.map { entry ->
            Rule(entry.key, entry.value)
        }
    }

    //endregion

    //region Behaviors

    fun enableBehavior(guild: ULong, channel: ULong, behavior: String)
    {
        getConfigForGuild(guild).behaviorSettings.getOrPut(channel) { mutableSetOf() }.add(behavior)
        storeConfigForGuild(guild)
    }

    fun enableAllBehaviors(guild: ULong, channel: ULong)
    {
        getConfigForGuild(guild).behaviorSettings[channel] = BehaviorCommand.behaviorNames.toMutableSet()
        storeConfigForGuild(guild)
    }

    fun disableBehavior(guild: ULong, channel: ULong, behavior: String)
    {
        getConfigForGuild(guild).behaviorSettings[channel]?.remove(behavior)
        storeConfigForGuild(guild)
    }

    fun disableAllBehaviors(guild: ULong, channel: ULong)
    {
        getConfigForGuild(guild).behaviorSettings[channel] = mutableSetOf()
        storeConfigForGuild(guild)
    }

    fun isBehaviorEnabled(guild: ULong, channel: ULong, behavior: String): Boolean
    {
        return getConfigForGuild(guild).behaviorSettings[channel]?.contains(behavior) ?: return false
    }

    fun getEnabledBehaviors(guild: ULong, channel: ULong): Set<String>
    {
        return getConfigForGuild(guild).behaviorSettings[channel]?.toSet() ?: emptySet()
    }

    //endregion
}

/**
 * @param applicationCommands map of internal id to command id as decided by Discord
 */
@Serializable
data class BotConfigStructure(
    val applicationCommands: MutableMap<String, Snowflake> = mutableMapOf()
)

@Serializable
data class GuildConfigStructure(
    var adminRoleId: ULong? = null,
    var keyValueStorage: MutableMap<String, String> = mutableMapOf(),
    // For the UwU translations
    var uwurules: MutableMap<String, String> = mutableMapOf(
        "[lr]" to "w",
        "[LR]" to "W",
        "This" to "Tis",
        "The" to "Te",
        "this" to "tis",
        "the" to "te",
        "No" to "Nyo",
        "no" to "nyo"
    ),
    var behaviorSettings: MutableMap<ULong, MutableSet<String>> = mutableMapOf(), // key is Channel Snowflake, value is list of enabled behaviors
)