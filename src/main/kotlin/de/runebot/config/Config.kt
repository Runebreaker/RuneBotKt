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

    private fun saveToFile()
    {
        Files.writeString(pathToConfigFile, serializer.encodeToString(config))
    }
}

@Serializable
class ConfigStructure(
    val keyValueStorage: MutableMap<String, String> = mutableMapOf(),
    val uwurules: MutableMap<String, String> = mutableMapOf()
)