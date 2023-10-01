package me.mnlr.vripper.download

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.LogEvent.Status.*
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.exception.PostParseException
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.model.PostItem
import me.mnlr.vripper.parser.ThreadLookupAPIResponseHandler
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.services.*
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.function.CheckedSupplier
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URISyntaxException
import java.util.*
import javax.xml.parsers.SAXParserFactory

class PostDownloadRunnable(private val threadId: String, private val postId: String) : KoinComponent, Runnable {
    private val log by LoggerDelegate()
    private val dataTransaction: DataTransaction by inject()
    private val settingsService: SettingsService by inject()
    private val vgAuthService: VGAuthService by inject()
    private val eventRepository: LogEventRepository by inject()
    private val cm: HTTPService by inject()
    private val retryPolicyService: RetryPolicyService by inject()
    private val downloadService: DownloadService by inject()
    private val link: String =
        "${settingsService.settings.viperSettings.host}/threads/$threadId?p=$postId"
    private val logEvent: LogEvent

    init {
        logEvent = eventRepository.save(
            LogEvent(
                type = LogEvent.Type.POST,
                status = PENDING,
                message = "Processing $link"
            )
        )
    }

    override fun run() {
        try {
            eventRepository.update(logEvent.copy(status = PROCESSING))
            if (dataTransaction.exists(postId)) {
                log.warn(String.format("skipping %s, already loaded", postId))
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR,
                        message = String.format("Gallery %s is already loaded", link)
                    )
                )
                return
            }
            val postItem: PostItem = try {
                parse()
            } catch (e: PostParseException) {
                val error = String.format("parsing failed for gallery %s", link)
                log.error(error, e)
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR, message = """
                    $error
                    ${e.formatToString()}
                    """.trimIndent()
                    )
                )
                return
            }
            if (postItem.imageItemList.isEmpty()) {
                val error = String.format("Gallery %s contains no images to download", link)
                log.error(error)
                eventRepository.update(logEvent.copy(status = ERROR, message = error))
                return
            }
            val post = dataTransaction.newPost(postItem)
            vgAuthService.leaveThanks(post)
//            metadataService.startFetchingMetadata(post)
            if (settingsService.settings.downloadSettings.autoStart) {
                log.debug("Auto start downloads option is enabled")
                downloadService.restartAll(listOf(postItem.postId))
                log.debug(String.format("Done enqueuing jobs for %s", postItem.url))
            }

            eventRepository.update(
                logEvent.copy(
                    status = DONE,
                    message = String.format(
                        "Gallery %s is successfully added to download queue",
                        link
                    )
                )
            )
        } catch (e: Exception) {
            val error = String.format("Error when adding gallery %s", link)
            log.error(error, e)
            eventRepository.update(
                logEvent.copy(
                    status = ERROR, message = """
                $error
                ${e.formatToString()}
                """.trimIndent()
                )
            )
        }
    }

    @Throws(PostParseException::class)
    fun parse(): PostItem {
        log.debug("Parsing post $postId")
        val httpGet: HttpGet = try {
            val uriBuilder = URIBuilder("${settingsService.settings.viperSettings.host}/vr.php")
            uriBuilder.setParameter("p", postId)
            cm.buildHttpGet(uriBuilder.build(), HttpClientContext.create())
        } catch (e: URISyntaxException) {
            throw PostParseException(e)
        }
        val threadLookupAPIResponseHandler = ThreadLookupAPIResponseHandler()
        log.debug("Requesting $httpGet")
        return try {
            Failsafe.with(retryPolicyService.buildGenericRetryPolicy<Any>()).onFailure {
                log.error(
                    "parsing failed for thread $threadId, post $postId", it.failure
                )
            }.get(CheckedSupplier {
                val connection: HttpClient = cm.client.build()
                (connection.execute(
                    httpGet, vgAuthService.context
                ) as CloseableHttpResponse).use { response ->
                    try {
                        if (response.statusLine.statusCode / 100 != 2) {
                            throw DownloadException("Unexpected response code '${response.statusLine.statusCode}' for $httpGet")
                        }
                        factory.newSAXParser()
                            .parse(response.entity.content, threadLookupAPIResponseHandler)
                        threadLookupAPIResponseHandler.result.postItemList[0]
                    } finally {
                        EntityUtils.consumeQuietly(response.entity)
                    }
                }
            })
        } catch (e: Exception) {
            throw PostParseException(e)
        }
    }

    companion object {
        private val factory = SAXParserFactory.newInstance()
    }
}
