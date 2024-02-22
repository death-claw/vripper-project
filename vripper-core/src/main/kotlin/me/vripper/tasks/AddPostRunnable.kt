package me.vripper.tasks

import me.vripper.download.DownloadService
import me.vripper.entities.LogEntry.Status.*
import me.vripper.exception.DownloadException
import me.vripper.exception.PostParseException
import me.vripper.model.PostItem
import me.vripper.model.ThreadPostId
import me.vripper.parser.ThreadLookupAPIResponseHandler
import me.vripper.services.*
import me.vripper.utilities.Tasks
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.function.CheckedSupplier
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.net.URIBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import javax.xml.parsers.SAXParserFactory

class AddPostRunnable(private val items: List<ThreadPostId>) : KoinComponent, Runnable {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val dataTransaction: DataTransaction by inject()
    private val settingsService: SettingsService by inject()
    private val vgAuthService: VGAuthService by inject()
    private val httpService: HTTPService by inject()
    private val retryPolicyService: RetryPolicyService by inject()
    private val downloadService: DownloadService by inject()
    private val cacheService: ThreadCacheService by inject()
    private val metadataService: MetadataService by inject()

    override fun run() {
        try {
            Tasks.increment()
            val toProcess = mutableListOf<PostItem>()
            for ((threadId, postId) in items) {
                if (dataTransaction.exists(postId)) {
                    log.warn(String.format("skipping %s, already loaded", postId))
                    continue
                }

                val threadItem = cacheService[threadId]
                val postItem: PostItem =
                    threadItem.postItemList.find { it.postId == postId } ?: parse(postId, threadId)
                toProcess.add(postItem)
            }

            val posts = dataTransaction.newPosts(toProcess.toList())
            posts.forEach {
                metadataService.fetchMetadata(it.postId)
            }
            if (settingsService.settings.downloadSettings.autoStart) {
                log.debug("Auto start downloads option is enabled")
                downloadService.restartAll(posts)
            }
        } catch (e: Exception) {
            val error = String.format("Error when adding galleries")
            log.error(error, e)
        } finally {
            Tasks.decrement()
        }
    }

    @Throws(PostParseException::class)
    fun parse(postId: Long, threadId: Long): PostItem {
        log.debug("Parsing post $postId")
        val httpGet =
            HttpGet(URIBuilder("${settingsService.settings.viperSettings.host}/vr.php").also {
                it.setParameter(
                    "p", postId.toString()
                )
            }.build())
        val threadLookupAPIResponseHandler = ThreadLookupAPIResponseHandler()
        log.debug("Requesting {}", httpGet)
        return try {
            Failsafe.with(retryPolicyService.buildGenericRetryPolicy<Any>()).onFailure {
                log.error(
                    "parsing failed for thread $threadId, post $postId", it.failure
                )
            }.get(CheckedSupplier {
                val connection: HttpClient = httpService.client
                connection.execute(
                    httpGet, vgAuthService.context
                ) { response ->
                    if (response.code / 100 != 2) {
                        throw DownloadException("Unexpected response code '${response.code}' for $httpGet")
                    }
                    factory.newSAXParser()
                        .parse(response.entity.content, threadLookupAPIResponseHandler)
                    threadLookupAPIResponseHandler.result.postItemList.first()
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
