package me.vripper.host

import me.vripper.download.ImageDownloadContext
import me.vripper.exception.HostException
import me.vripper.exception.XpathException
import me.vripper.services.*
import me.vripper.utilities.LoggerDelegate
import me.vripper.utilities.XpathUtils
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.util.*

internal class PostImgHost(
    httpService: HTTPService,
    dataTransaction: DataTransaction,
    downloadSpeedService: DownloadSpeedService,
) : Host("postimg.cc", 13, httpService, dataTransaction, downloadSpeedService) {
    private val log by LoggerDelegate()

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        val titleNode = try {
            log.debug(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url))
            XpathUtils.getAsNode(document, TITLE_XPATH)
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
            val imgTitle = Optional.ofNullable(titleNode)
                .map { node: Node -> node.textContent.trim { it <= ' ' } }
                .orElseGet { getDefaultImageName(url) }
            Pair(imgTitle, urlNode.attributes.getNamedItem("href").textContent.trim { it <= ' ' })
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val TITLE_XPATH = "//span[contains(@class,'imagename')]"
        private const val IMG_XPATH = "//a[@id='download']"
    }
}