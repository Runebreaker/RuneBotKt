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

    private val config: ConfigEntries
    private val pathToConfigFile: Path = Path("config.json")

    init
    {
        if (!Files.exists(pathToConfigFile))
        {
            Files.createFile(pathToConfigFile)
            Files.writeString(pathToConfigFile, serializer.encodeToString(ConfigEntries()))
        }
        config = serializer.decodeFromString(pathToConfigFile.toFile().readText())
    }

    fun store(key: String, value: String)
    {
        config.values[key] = value
        Files.writeString(pathToConfigFile, serializer.encodeToString(config))
    }

    fun storeRule(key: String, value: String)
    {
        config.uwurules[key] = value
        Files.writeString(pathToConfigFile, serializer.encodeToString(config))
    }

    fun get(key: String): String? = config.values[key]

    fun getRules(): List<UwuifyCommand.Rule>
    {
        return config.uwurules.map { entry ->
            UwuifyCommand.Rule(entry.key, entry.value)
        }
    }
}

@Serializable
class ConfigEntries(
    val values: MutableMap<String, String> = mutableMapOf(),
    val uwurules: MutableMap<String, String> = mutableMapOf()
)