package me.mnlr.vripper.host

import org.springframework.stereotype.Service
import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*
import java.net.URI
import java.net.URISyntaxException

@Service
class PimpandhostHost(
    private val xpathService: XpathService,
    private val htmlProcessorService: HtmlProcessorService,
    httpService: HTTPService,
    dataTransaction: DataTransaction,
    downloadSpeedService: DownloadSpeedService,
) : Host(httpService, htmlProcessorService, dataTransaction, downloadSpeedService) {
    private val log by LoggerDelegate()
    override val host: String
        get() = Companion.host

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        val newUrl: String
        try {
            newUrl = appendUri(url.replace("http://", "https://"), "size=original")
        } catch (e: Exception) {
            throw HostException(e)
        }
        val doc = fetch(newUrl, context) {
            htmlProcessorService.clean(it.entity.content)
        }
        val imgNode: Node = try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, newUrl))
            xpathService.getAsNode(doc, IMG_XPATH)
        } catch (e: XpathException) {
            throw HostException(e)
        } ?: throw HostException(
            String.format(
                "Xpath '%s' cannot be found in '%s'",
                IMG_XPATH,
                url
            )
        )
        return try {
            log.debug(String.format("Resolving name and image url for %s", newUrl))
            val imgTitle = imgNode.attributes.getNamedItem("alt").textContent.trim { it <= ' ' }
            val imgUrl =
                "https:" + imgNode.attributes.getNamedItem("src").textContent.trim { it <= ' ' }
            Pair(
                imgTitle.ifEmpty { imgUrl.substring(imgUrl.lastIndexOf('/') + 1) }, imgUrl
            )
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    @Throws(URISyntaxException::class)
    fun appendUri(uri: String, appendQuery: String): String {
        val oldUri = URI(uri)
        var newQuery = oldUri.query
        if (newQuery == null) {
            newQuery = appendQuery
        } else {
            newQuery += "&$appendQuery"
        }
        return URI(
            oldUri.scheme, oldUri.authority, oldUri.path, newQuery, oldUri.fragment
        ).toString()
    }

    companion object {
        private const val host = "pimpandhost.com"
        private const val lookup = "pimpandhost.com"
        private const val IMG_XPATH = "//img[contains(@class, 'original')]"
    }
}