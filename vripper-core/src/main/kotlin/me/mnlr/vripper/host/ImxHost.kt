package me.mnlr.vripper.host

import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.w3c.dom.Document
import org.w3c.dom.Node
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.exception.HtmlProcessorException
import me.mnlr.vripper.exception.XpathException
import me.mnlr.vripper.services.*
import java.io.IOException

class ImxHost(
    private val httpService: HTTPService,
    dataTransaction: DataTransaction,
    globalStateService: GlobalStateService,
) : Host("imx.to", httpService, dataTransaction, globalStateService) {
    private val log by LoggerDelegate()

    @Throws(HostException::class)
    override fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String> {
        var value: String? = null
        try {
            log.debug("Looking for xpath expression $CONTINUE_BUTTON_XPATH in $url")
            val contDiv = XpathService.getAsNode(document, CONTINUE_BUTTON_XPATH)
                ?: throw HostException("$CONTINUE_BUTTON_XPATH cannot be found")
            val node = contDiv.attributes.getNamedItem("value")
            if (node != null) {
                value = node.textContent
            }
        } catch (e: XpathException) {
            throw HostException(e)
        }
        if (value == null) {
            throw HostException("Failed to obtain value attribute from continue input")
        }
        log.debug("Click button found for $url")
        val client: HttpClient = httpService.client.build()
        val httpPost: HttpPost = httpService.buildHttpPost(url, context.httpContext)
        val params: MutableList<NameValuePair> = ArrayList()
        params.add(BasicNameValuePair("imgContinue", value))
        try {
            httpPost.entity = UrlEncodedFormEntity(params)
        } catch (e: Exception) {
            throw HostException(e)
        }
        log.debug("Requesting $httpPost")
        val doc = try {
            (client.execute(
                httpPost, context.httpContext
            ) as CloseableHttpResponse).use { response ->
                log.debug("Cleaning response for $httpPost")
                try {
                    HtmlProcessorService.clean(response.entity.content)
                } finally {
                    EntityUtils.consumeQuietly(response.entity)
                }
            }
        } catch (e: IOException) {
            throw HostException(e)
        } catch (e: HtmlProcessorException) {
            throw HostException(e)
        }
        val imgNode: Node = try {
            log.debug("Looking for xpath expression $IMG_XPATH in $url")
            XpathService.getAsNode(doc, IMG_XPATH)
        } catch (e: XpathException) {
            throw HostException(e)
        } ?: throw HostException(
            "Xpath $IMG_XPATH cannot be found in $url"
        )
        return try {
            log.debug("Resolving name and image url for $url")
            val imgTitle = imgNode.attributes.getNamedItem("alt").textContent.trim { it <= ' ' }
            val imgUrl = imgNode.attributes.getNamedItem("src").textContent.trim { it <= ' ' }
            Pair(
                imgTitle.ifEmpty { getDefaultImageName(imgUrl) }, imgUrl
            )
        } catch (e: Exception) {
            throw HostException("Unexpected error occurred", e)
        }
    }

    companion object {
        private const val CONTINUE_BUTTON_XPATH = "//*[@name='imgContinue']"
        private const val IMG_XPATH = "//img[@class='centred']"
    }
}