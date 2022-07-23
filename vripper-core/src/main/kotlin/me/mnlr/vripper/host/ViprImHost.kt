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
class ViprImHost(
    private val xpathService: XpathService,
    httpService: HTTPService,
    htmlProcessorService: HtmlProcessorService,
    dataTransaction: DataTransaction,
    downloadSpeedService: DownloadSpeedService,
) : Host(httpService, htmlProcessorService, dataTransaction, downloadSpeedService) {
    private val log by LoggerDelegate()
    override val host: String = "vipr.im"

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        val imgNode: Node = try {
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
            val imgTitle =
                Optional.ofNullable(imgNode.attributes.getNamedItem("alt"))
                    .map { obj: Node -> obj.textContent }
                    .map { obj: String -> obj.trim() }.orElse(null)
            val imgUrl = imgNode.attributes.getNamedItem("src").textContent.trim()
            Pair(imgTitle!!, imgUrl)
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val IMG_XPATH = "//img[contains(@class, 'img')]"
    }
}