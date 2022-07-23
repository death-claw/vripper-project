package me.mnlr.vripper.host

import org.springframework.stereotype.Service
import org.w3c.dom.Document
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*

@Service
class PixxxelsHost(
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
        val imgNode = try {
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
        return try {
            log.debug(String.format("Resolving name and image url for %s", url))
            val imgTitle = titleNode.textContent.trim { it <= ' ' }
            val imgUrl = imgNode.attributes.getNamedItem("href").textContent.trim { it <= ' ' }
            Pair(
                imgTitle.ifEmpty { imgUrl.substring(imgUrl.lastIndexOf('/') + 1) }, imgUrl
            )
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val host = "pixxxels.cc"
        private const val lookup = "pixxxels.cc"
        private const val IMG_XPATH = "//*[@id='download']"
        private const val TITLE_XPATH = "//*[contains(@class,'imagename')]"
    }
}