package me.mnlr.vripper.host

import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*
import java.util.*

class DPicMeHost(
    httpService: HTTPService,
    dataTransaction: DataTransaction,
    globalStateService: GlobalStateService,
) : Host("dpic.me", httpService, dataTransaction, globalStateService) {
    private val log by LoggerDelegate()

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        val imgNode: Node = try {
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
        return try {
            log.debug(String.format("Resolving name and image url for %s", url))
            val imgTitle =
                Optional.ofNullable(imgNode.attributes.getNamedItem("alt"))
                    .map { e: Node -> e.textContent.trim() }
                    .orElse("")
            val imgUrl =
                Optional.ofNullable(imgNode.attributes.getNamedItem("src"))
                    .map { e: Node -> e.textContent.trim() }
                    .orElse("")
            var defaultName: String = UUID.randomUUID().toString()
            val index = imgUrl.lastIndexOf('/')
            if (index != -1 && index < imgUrl.length) {
                defaultName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1)
            }
            Pair((imgTitle.ifEmpty { defaultName })!!, imgUrl)
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val IMG_XPATH = "//img[@id='pic']"
    }
}