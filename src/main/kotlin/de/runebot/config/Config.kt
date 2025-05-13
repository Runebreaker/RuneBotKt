package de.runebot.config

import de.runebot.Util.Rule
import de.runebot.commands.BehaviorCommand
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

    private val configs: MutableMap<ULong, ConfigStructure> = mutableMapOf()
    private val pathToConfigDir = Path("config/")

    init
    {
        if (Files.notExists(pathToConfigDir))
        {
            Files.createDirectory(pathToConfigDir)
        }

        pathToConfigDir.listDirectoryEntries().forEach { path ->
            configs[path.name.removeSuffix(".json").toULongOrNull() ?: return@forEach] = serializer.decodeFromString(path.toFile().readText())
        }
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

    fun getConfigForGuild(id: ULong): ConfigStructure
    {
        return configs.getOrPut(id) { ConfigStructure() }
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

@Serializable
data class ConfigStructure(
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