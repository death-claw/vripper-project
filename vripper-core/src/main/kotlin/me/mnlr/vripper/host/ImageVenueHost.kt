package me.mnlr.vripper.host

import org.springframework.stereotype.Service
import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*

@Service
class ImageVenueHost(
    private val htmlProcessorService: HtmlProcessorService,
    private val xpathService: XpathService,
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
        val doc = try {
            log.debug(
                String.format(
                    "Looking for xpath expression %s in %s",
                    CONTINUE_BUTTON_XPATH,
                    url
                )
            )
            if (xpathService.getAsNode(document, CONTINUE_BUTTON_XPATH) != null) {
                // Button detected. No need to actually click it, just make the call again.
                fetch(url, context) {
                    htmlProcessorService.clean(it.entity.content)
                }
            } else {
                document
            }
        } catch (e: XpathException) {
            throw HostException(e)
        }
        val imgNode: Node = try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url))
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
            log.debug(String.format("Resolving name and image url for %s", url))
            val imgTitle = imgNode.attributes.getNamedItem("alt").textContent.trim { it <= ' ' }
            val imgUrl = imgNode.attributes.getNamedItem("src").textContent.trim { it <= ' ' }
            Pair(
                if (imgTitle.isEmpty()) imgUrl.substring(imgUrl.lastIndexOf('/') + 1) else imgTitle,
                imgUrl
            )
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val host = "imagevenue.com"
        private const val lookup = "imagevenue.com"
        private const val CONTINUE_BUTTON_XPATH = "//a[@title='Continue to ImageVenue']"
        private const val IMG_XPATH = "//a[@data-toggle='full']/img"
    }
}