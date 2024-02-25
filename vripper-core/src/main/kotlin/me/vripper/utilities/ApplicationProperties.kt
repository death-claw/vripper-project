package me.vripper.utilities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.vripper.exception.DownloadException
import me.vripper.services.HTTPService
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

object ApplicationProperties : KoinComponent {
    const val VERSION: String = "5.3.0"
    private const val BASE_DIR_NAME: String = "vripper"
    private val portable = System.getProperty("vripper.portable", "true").toBoolean()
    private val BASE_DIR: String = getBaseDir()
    private val httpService: HTTPService by inject()
    val VRIPPER_DIR: Path = Path(BASE_DIR, BASE_DIR_NAME)
    private val json = Json {
        ignoreUnknownKeys = true
    }

    init {
        Files.createDirectories(VRIPPER_DIR)
        System.setProperty("VRIPPER_DIR", VRIPPER_DIR.toRealPath().pathString)
    }

    fun latestVersion(): String {
        @Serializable
        data class ReleaseResponse(@SerialName("tag_name") val tagName: String)

        val httpGet = HttpGet("https://api.github.com/repos/death-claw/vripper-project/releases/latest")
        return httpService.client.execute(httpGet) {
            if (it.code / 100 != 2) {
                throw DownloadException("Unexpected response code '${it.code}' for $httpGet")
            }
            json.decodeFromString<ReleaseResponse>(EntityUtils.toString(it.entity)).tagName
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
