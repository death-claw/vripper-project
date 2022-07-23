package me.mnlr.vripper.tasks

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.util.EntityUtils
import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.LogEvent.Status.*
import me.mnlr.vripper.entities.Metadata
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.services.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class MetadataRunnable(private val postDownloadState: PostDownloadState) : Runnable {

    private val log by LoggerDelegate()
    private val dataTransaction: DataTransaction =
        SpringContext.getBean(DataTransaction::class.java)
    private val eventRepository: LogEventRepository =
        SpringContext.getBean(LogEventRepository::class.java)
    private val cm: HTTPService = SpringContext.getBean(HTTPService::class.java)
    private val htmlProcessorService: HtmlProcessorService =
        SpringContext.getBean(HtmlProcessorService::class.java)
    private val xpathService: XpathService = SpringContext.getBean(XpathService::class.java)
    private val metadataService: MetadataService =
        SpringContext.getBean(MetadataService::class.java)
    private val context: HttpClientContext = HttpClientContext.create()
    private val logEvent: LogEvent

    @Volatile
    private var stopped = false

    @Volatile
    var finished = false

    init {
        val vgAuthService: VGAuthService = SpringContext.getBean(VGAuthService::class.java)
        context.cookieStore = vgAuthService.context.cookieStore
        context.setAttribute(
            HTTPService.ContextAttributes.CONTEXT_ATTRIBUTES, HTTPService.ContextAttributes()
        )
        logEvent = eventRepository.save(
            LogEvent(
                type = LogEvent.Type.METADATA,
                status = PENDING,
                message = "Fetching metadata for " + postDownloadState.url
            )
        )
    }

    override fun run() {
        try {
            eventRepository.update(logEvent.copy(status = PROCESSING))
            val metadata = fetchMetadata(postDownloadState.postId, postDownloadState.url)
            dataTransaction.setMetadata(postDownloadState, metadata)
            eventRepository.update(logEvent.copy(status = DONE))
        } catch (e: Exception) {
            val message = "Failed to fetch metadata for ${postDownloadState.url}"
            log.error(message, e)
            eventRepository.update(
                logEvent.copy(
                    status = ERROR, message = """
                $message
                ${e.formatToString()}
                """.trimIndent()
                )
            )
        } finally {
            if (stopped) {
                val message = "Fetching metadata for ${postDownloadState.url} interrupted"
                eventRepository.update(logEvent.copy(status = DONE, message = message))
            }
            finished = true
            metadataService.stopFetchingMetadata(listOf(postDownloadState.postId))
        }
    }

    private fun fetchMetadata(postId: String, url: String): Metadata {
        val httpGet: HttpGet = cm.buildHttpGet(url, context)
        val connection: HttpClient = cm.client.build()
        (connection.execute(httpGet, context) as CloseableHttpResponse).use { response ->
            try {
                if (response.statusLine.statusCode / 100 != 2) {
                    throw DownloadException("Unexpected response code '${response.statusLine.statusCode}' for $httpGet")
                }
                val document: Document =
                    htmlProcessorService.clean(response.entity.content)
                val postNode: Node? =
                    xpathService.getAsNode(
                        document,
                        "//li[@id='post_$postId']/div[contains(@class, 'postdetails')]"
                    )
                val postedBy: String = xpathService.getAsNode(
                    postNode,
                    "./div[contains(@class, 'userinfo')]//a[contains(@class, 'username')]//font"
                )?.textContent?.trim() ?: ""

                val metadata = Metadata()
                metadata.postedBy = postedBy

                val node: Node? = xpathService.getAsNode(
                    document, "//div[@id='post_message_$postId']"
                )
                if (node != null) {
                    metadata.resolvedNames = findTitleInContent(node)
                } else {
                    metadata.resolvedNames = emptyList()
                }
                metadata.postId = postId
                return metadata
            } finally {
                EntityUtils.consumeQuietly(response.entity)
            }
        }
    }

    private fun findTitleInContent(node: Node): List<String> {
        val altTitle: MutableList<String> = ArrayList()
        findTitle(node, altTitle, AtomicBoolean(true))
        return altTitle.stream().distinct().collect(Collectors.toList())
    }

    private fun findTitle(node: Node, altTitle: MutableList<String>, keepGoing: AtomicBoolean) {
        if (!keepGoing.get()) {
            return
        }
        if (node.nodeName == "a" || node.nodeName == "img") {
            keepGoing.set(false)
            return
        }
        if (node.nodeType == Node.ELEMENT_NODE) {
            for (i in 0 until node.childNodes.length) {
                val item = node.childNodes.item(i)
                findTitle(item, altTitle, keepGoing)
                if (!keepGoing.get()) {
                    return
                }
            }
        } else if (node.nodeType == Node.TEXT_NODE) {
            val text = node.textContent.trim()
            if (text.isNotBlank() && dictionary.stream().noneMatch { e: String ->
                    text.lowercase().contains(e.lowercase())
                }) {
                altTitle.add(text)
            }
        }
    }

    fun stop() {
        stopped = true
        val contextAttributes: HTTPService.ContextAttributes? = context.getAttribute(
            HTTPService.ContextAttributes.CONTEXT_ATTRIBUTES,
            HTTPService.ContextAttributes::class.java
        )
        if (contextAttributes != null) {
            synchronized(contextAttributes.requests) {
                for (request in contextAttributes.requests) {
                    request.abort()
                }
            }
        }
    }

    companion object {
        private val dictionary: List<String> =
            mutableListOf("download", "link", "rapidgator", "filefactory", "filefox")
    }
}