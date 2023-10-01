package me.mnlr.vripper.host

import org.w3c.dom.Document
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*

class ImageZillaHost(
    httpService: HTTPService,
    dataTransaction: DataTransaction,
    globalStateService: GlobalStateService,
) : Host("imagezilla.net", httpService, dataTransaction, globalStateService) {
    private val log by LoggerDelegate()

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        val titleNode = try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url))
            XpathService.getAsNode(document, IMG_XPATH)
        } catch (e: XpathException) {
            throw HostException(e)
        } ?: throw HostException(
            String.format(
                "Xpath '%s' cannot be found in '%s'",
                IMG_XPATH,
                url
            )
        )
        log.debug(String.format("Resolving name for %s", url))
        var title = titleNode.attributes.getNamedItem("title").textContent.trim()
        titleNode.textContent.trim()
        if (title.isEmpty()) {
            title = getDefaultImageName(url)
        }
        return try {
            Pair(title, url.replace("show", "images"))
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val IMG_XPATH = "//img[@id='photo']"
    }
}