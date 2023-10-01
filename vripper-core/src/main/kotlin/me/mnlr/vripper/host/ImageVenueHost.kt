package me.mnlr.vripper.host

import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*

class ImageVenueHost(
    httpService: HTTPService,
    dataTransaction: DataTransaction,
    globalStateService: GlobalStateService,
) : Host("imagevenue.com", httpService, dataTransaction, globalStateService) {
    private val log by LoggerDelegate()

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
            if (XpathService.getAsNode(document, CONTINUE_BUTTON_XPATH) != null) {
                // Button detected. No need to actually click it, just make the call again.
                fetch(url, context) {
                    HtmlProcessorService.clean(it.entity.content)
                }
            } else {
                document
            }
        } catch (e: XpathException) {
            throw HostException(e)
        }
        val imgNode: Node = try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url))
            XpathService.getAsNode(doc, IMG_XPATH)
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
        private const val CONTINUE_BUTTON_XPATH = "//a[@title='Continue to ImageVenue']"
        private const val IMG_XPATH = "//a[@data-toggle='full']/img"
    }
}