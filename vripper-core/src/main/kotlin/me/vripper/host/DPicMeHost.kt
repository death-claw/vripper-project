package me.vripper.host

import me.vripper.download.ImageDownloadContext
import me.vripper.exception.HostException
import me.vripper.exception.XpathException
import me.vripper.services.*
import me.vripper.utilities.XpathUtils
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.util.*

class DPicMeHost(
    httpService: HTTPService,
    dataTransaction: DataTransaction,
    downloadSpeedService: DownloadSpeedService,
) : Host("dpic.me", 1, httpService, dataTransaction, downloadSpeedService) {
    private val log by me.vripper.delegate.LoggerDelegate()

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        val imgNode: Node = try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url))
            XpathUtils.getAsNode(document, IMG_XPATH)
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