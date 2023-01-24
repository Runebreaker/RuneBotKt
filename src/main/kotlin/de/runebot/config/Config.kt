package de.runebot.config

import de.runebot.commands.UwuifyCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

object Config
{
    private val serializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val config: ConfigStructure
    private val pathToConfigFile: Path = Path("config.json")

    private val defaultConfig = ConfigStructure(
        keyValueStorage = mutableMapOf(),
        uwurules = mutableMapOf(
            "[lr]" to "w",
            "[LR]" to "W",
            "This" to "Tis",
            "The" to "Te",
            "this" to "tis",
            "the" to "te"
        )
    )

    init
    {
        if (!Files.exists(pathToConfigFile))
        {
            Files.createFile(pathToConfigFile)
            Files.writeString(pathToConfigFile, serializer.encodeToString(defaultConfig))
        }
        config = serializer.decodeFromString(pathToConfigFile.toFile().readText())
    }

    //region KeyValueStorage

    fun storeValue(key: String, value: String)
    {
        config.keyValueStorage[key] = value
        saveToFile()
    }

    fun resetValue(key: String)
    {
        config.keyValueStorage.remove(key)
        saveToFile()
    }

    fun getValue(key: String): String?
    {
        return config.keyValueStorage[key]
    }

    //endregion

    //region UwURules

    fun storeRule(key: String, value: String)
    {
        config.uwurules[key] = value
        saveToFile()
    }

    fun resetRule(key: String)
    {
        config.uwurules.remove(key)
        saveToFile()
    }

    fun getRules(): List<UwuifyCommand.Rule>
    {
        return config.uwurules.map { entry ->
            UwuifyCommand.Rule(entry.key, entry.value)
        }
    }

    //endregion

    //region DisabledBehaviourChannels

    fun storeEnabledBehaviour(guild: ULong, channel: ULong, behaviour: String)
    {
        config.guildConfigs.getOrPut(guild) { GuildConfig() }.channelConfigs.getOrPut(channel) { ChannelConfig() }.behaviourEnables.add(behaviour)
        saveToFile()
    }

    fun resetEnabledBehaviour(guild: ULong, channel: ULong, behaviour: String): Boolean
    {
        val retVal = config.guildConfigs.getOrElse(guild) { return false }.channelConfigs.getOrElse(channel) { return false }.behaviourEnables.remove(behaviour)
        saveToFile()
        return retVal
    }

    fun getEnabledBehaviour(guild: ULong, channel: ULong, behaviour: String): Boolean
    {
        return config.guildConfigs.getOrElse(guild) { return false }.channelConfigs.getOrElse(channel) { return false }.behaviourEnables.contains(behaviour)
    }

    //endregion

    private fun saveToFile()
    {
        Files.writeString(pathToConfigFile, serializer.encodeToString(config))
    }
}

@Serializable
class ConfigStructure(
    val keyValueStorage: MutableMap<String, String> = mutableMapOf(),
    val uwurules: MutableMap<String, String> = mutableMapOf(),
    // Guilds structure: Guilds > Channels > Toggles
    val guildConfigs: MutableMap<ULong, GuildConfig> = mutableMapOf()
)

@Serializable
class ChannelConfig(
    val behaviourEnables: MutableSet<String> = mutableSetOf()
)

@Serializable
class GuildConfig(
    val channelConfigs: MutableMap<ULong, ChannelConfig> = mutableMapOf()
)