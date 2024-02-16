package de.runebot.commands

import de.runebot.Util
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


object AnilistCommand : MessageCommandInterface
{
    private val search = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(listOf("search", "s"), Pair("search <name>", "Searches AniList for the provided name.")),
        { event, args, _ ->
            val values = mapOf("query" to defaultQuery(), "variables" to defaultSearchVariables(args.joinToString(" ")))
            val requestBody: String = serializer.encodeToString(values)

            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(anilistUrl))
                .headers("Content-Type", "application/json", "Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val result = serializer.decodeFromString<Root>(response.body())
            val bobTheStringBuilder = StringBuilder("Found results:\n")
            result.data.Page.media.forEach { media ->
                media.title.english?.let { bobTheStringBuilder.append(it).append("//") }
                media.title.native?.let { bobTheStringBuilder.append(it).append("//") }
                media.title.romaji?.let { bobTheStringBuilder.append(it).append("//") }
                bobTheStringBuilder.trim { it == '/' }
                bobTheStringBuilder.append(System.lineSeparator())
            }
            // Test
            Util.sendMessage(event, bobTheStringBuilder.toString())
        },
        emptyList()
    )
    private val anilist = MessageCommandInterface.Subcommand(
        MessageCommandInterface.CommandDescription(names, Pair(shortHelpText, "")),
        subcommands = listOf(search)
    )

    override val names: List<String>
        get() = listOf("anilist", "ani", "al")
    override val shortHelpText: String
        get() = "look up any content on anilist, including a variety of data"
    override val longHelpText: String
        get() = anilist.toTree().toString()

    private const val anilistUrl = "https://graphql.anilist.co"
    private val serializer = Json

    override suspend fun prepare(kord: Kord)
    {
        println("Anilist integration ready.")
    }

    override suspend fun execute(event: MessageCreateEvent, args: List<String>)
    {
        anilist.execute(event, args.subList(1, args.size), listOf(args[0].substring(1)))
    }

    private fun defaultQuery(): String
    {
        return """
            query (${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo {
                  currentPage
                }
                media(search: ${'$'}search, type: ANIME, sort:POPULARITY_DESC) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  genres
                  averageScore
                  meanScore
                  popularity
                }
              }
            }
        """.trimIndent()
    }

    private fun defaultSearchVariables(name: String, page: Int = 1, perPage: Int = 5): String
    {
        return """
            {
              "search": "$name",
              "page": $page,
              "perPage": $perPage
            }
        """.trimIndent()
    }

    @kotlinx.serialization.Serializable
    data class Root(val data: Data)

    @kotlinx.serialization.Serializable
    data class Data(val Page: Page)

    @kotlinx.serialization.Serializable
    data class Page(val pageInfo: PageInfo, val media: List<Media>)

    @kotlinx.serialization.Serializable
    data class PageInfo(val currentPage: Int)

    @kotlinx.serialization.Serializable
    data class Media(val id: Int, val title: MediaTitle, val genres: List<String>?, val averageScore: Int?, val meanScore: Int?, val popularity: Int?)

    @kotlinx.serialization.Serializable
    data class MediaTitle(val romaji: String?, val english: String?, val native: String?)
}