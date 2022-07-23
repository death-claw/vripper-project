package me.mnlr.vripper.host

import org.springframework.stereotype.Service
import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*
import java.util.*

@Service
class PostImgHost(
    private val xpathService: XpathService,
    httpService: HTTPService,
    htmlProcessorService: HtmlProcessorService,
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
        val titleNode = try {
            log.debug(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url))
            xpathService.getAsNode(document, TITLE_XPATH)
        } catch (e: XpathException) {
            throw HostException(e)
        } ?: throw HostException(
            String.format(
                "Xpath '%s' cannot be found in '%s'",
                TITLE_XPATH,
                url
            )
        )
        val urlNode = try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url))
            xpathService.getAsNode(document, IMG_XPATH)
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
            log.debug(String.format("Resolving name and image url for %s", url))
            val imgTitle = Optional.ofNullable(titleNode)
                .map { node: Node -> node.textContent.trim { it <= ' ' } }
                .orElseGet { getDefaultImageName(url) }
            Pair(imgTitle, urlNode.attributes.getNamedItem("href").textContent.trim { it <= ' ' })
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val host = "postimg.cc"
        private const val lookup = "postimg.cc"
        private const val TITLE_XPATH = "//span[contains(@class,'imagename')]"
        private const val IMG_XPATH = "//a[@id='download']"
    }
}