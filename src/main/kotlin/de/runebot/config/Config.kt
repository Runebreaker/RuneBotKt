package de.runebot.config

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
    private val pathToConfigFile: Path = Config::class.java.getResource("config.json")?.let { Path(it.path) } ?: Path("config.json") // resources is eh net gut

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

    fun get(key: String): String? = config.values[key]
}

@Serializable
class ConfigEntries(
    val values: MutableMap<String, String> = mutableMapOf()
)