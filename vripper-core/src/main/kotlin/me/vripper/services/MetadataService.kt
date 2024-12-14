package me.vripper.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import me.vripper.entities.MetadataEntity
import me.vripper.exception.DownloadException
import me.vripper.exception.VripperException
import me.vripper.tasks.Tasks
import me.vripper.utilities.HtmlUtils
import me.vripper.utilities.RequestLimit
import me.vripper.utilities.XpathUtils
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.net.URIBuilder
import org.w3c.dom.Node
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

internal class MetadataService(
    private val httpService: HTTPService,
    private val settingsService: SettingsService,
    private val dataTransaction: DataTransaction,
    private val vgAuthService: VGAuthService,
) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dictionary: List<String> = mutableListOf("download", "link", "rapidgator", "filefactory", "filefox")

    fun init() {
        dataTransaction.findAllPosts().filter { dataTransaction.findMetadataByPostId(it.postId).isEmpty }
            .map { it.postId }.forEach(::fetchMetadata)
    }

    fun fetchMetadata(postId: Long) {
        Tasks.increment()
        coroutineScope.launch {
            try {
                val httpGet = HttpGet(URIBuilder(settingsService.settings.viperSettings.host + "/threads/").also {
                    it.setParameter(
                        "p", postId.toString()
                    )
                }.build())

                val response = RequestLimit.semaphore.withPermit {
                    httpService.client.execute(httpGet, vgAuthService.context) {
                        if (it.code / 100 != 2) {
                            throw DownloadException("Unexpected response code '${it.code}' for $httpGet")
                        }
                        EntityUtils.toString(it.entity)
                    }
                }
                val document = HtmlUtils.clean(response)
                val postNode: Node = XpathUtils.getAsNode(
                    document,
                    "//li[@id='post_$postId']/div[contains(@class, 'postdetails')]",

                    ) ?: throw VripperException("Unable to find post #'$postId'")

                val postedBy: String = XpathUtils.getAsNode(
                    postNode, "./div[contains(@class, 'userinfo')]//a[contains(@class, 'username')]//font"
                )?.textContent?.trim()
                    ?: throw VripperException("Unable to find the poster for post #'$postId'")


                val node: Node = XpathUtils.getAsNode(
                    document, java.lang.String.format("//div[@id='post_message_%s']", postId)
                ) ?: throw VripperException("Unable to locate post content")
                val titles = findTitleInContent(node)
                val metadataEntity = MetadataEntity(postId, MetadataEntity.Data(postedBy, titles))
                dataTransaction.saveMetadata(metadataEntity)
            } finally {
                Tasks.decrement()
            }
        }
    }

    private fun findTitleInContent(node: Node): List<String> {
        val altTitle: MutableList<String> = mutableListOf()
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
            if (text.isNotBlank() && dictionary.stream().noneMatch { e ->
                    text.lowercase().contains(e.lowercase())
                }) {
                altTitle.add(text)
            }
        }
    }
}