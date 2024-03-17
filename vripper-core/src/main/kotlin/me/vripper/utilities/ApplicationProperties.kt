package me.vripper.utilities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.vripper.exception.DownloadException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

object ApplicationProperties {
    val VERSION: String =
        ApplicationProperties.javaClass.getResourceAsStream("/version")
            ?.use { inputStream -> inputStream.reader().use { it.readText() } } ?: "undefined"
    private const val BASE_DIR_NAME: String = "vripper"
    private val portable = System.getProperty("vripper.portable", "true").toBoolean()
    private val BASE_DIR: String = getBaseDir()
    val VRIPPER_DIR: Path = Path(BASE_DIR, BASE_DIR_NAME)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Serializable
    internal data class ReleaseResponse(@SerialName("tag_name") val tagName: String)

    init {
        Files.createDirectories(VRIPPER_DIR)
        System.setProperty("VRIPPER_DIR", VRIPPER_DIR.toRealPath().pathString)
    }

    fun latestVersion(): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/death-claw/vripper-project/releases/latest")).build()
        return HttpClient.newHttpClient().use {
            val response = it.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() / 100 != 2) {
                throw DownloadException("Unexpected response code '${response.statusCode()}' for $request")
            }
            json.decodeFromString<ReleaseResponse>(response.body()).tagName
        }
    }

    private fun getBaseDir(): String {
        return if (portable) {
            System.getProperty("user.dir")
        } else {
            val os = System.getProperty("os.name")
            if (os.contains("Windows")) {
                System.getProperty("user.home")
            } else if (os.contains("Linux")) {
                "${System.getProperty("user.home")}/.config"
            } else if (os.contains("Mac")) {
                "${System.getProperty("user.home")}/.config"
            } else {
                System.getProperty("user.dir")
            }
        }
    }
}
