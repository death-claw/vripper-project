package me.mnlr.vripper.parser

import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.function.CheckedSupplier
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.exception.PostParseException
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.services.HTTPService
import me.mnlr.vripper.services.RetryPolicyService
import me.mnlr.vripper.services.SettingsService
import me.mnlr.vripper.services.VGAuthService
import java.io.BufferedInputStream
import java.net.URISyntaxException
import java.util.*
import javax.xml.parsers.SAXParserFactory

class ThreadLookupAPIParser(private val threadId: String) {
    private val log by LoggerDelegate()
    private val cm: HTTPService = SpringContext.getBean(HTTPService::class.java)
    private val retryPolicyService: RetryPolicyService =
        SpringContext.getBean(RetryPolicyService::class.java)
    private val vgAuthService: VGAuthService = SpringContext.getBean(VGAuthService::class.java)
    private val settingsService: SettingsService =
        SpringContext.getBean(SettingsService::class.java)

    @Throws(PostParseException::class)
    fun parse(): ThreadItem {
        log.debug("Parsing thread $threadId")
        val httpGet: HttpGet = try {
            val uriBuilder = URIBuilder(settingsService.settings.viperSettings.host + "/vr.php")
            uriBuilder.setParameter("t", threadId)
            cm.buildHttpGet(uriBuilder.build(), HttpClientContext.create())
        } catch (e: URISyntaxException) {
            throw PostParseException(e)
        }
        val threadLookupAPIResponseHandler = ThreadLookupAPIResponseHandler()
        log.debug("Requesting $httpGet")
        return try {
            Failsafe.with(retryPolicyService.buildGenericRetryPolicy<Any>()).onFailure {
                log.error(
                    "parsing failed for thread $threadId",
                    it.failure
                )
            }.get(CheckedSupplier {
                val connection: HttpClient = cm.client.build()
                (connection.execute(
                    httpGet, vgAuthService.context
                ) as CloseableHttpResponse).use { response ->
                    try {
                        if (response.statusLine.statusCode / 100 != 2) {
                            throw DownloadException("Unexpected response code '${response.statusLine.statusCode}' for $httpGet")
                        }
                        factory.newSAXParser().parse(
                            BufferedInputStream(response.entity.content),
                            threadLookupAPIResponseHandler
                        )
                        threadLookupAPIResponseHandler.result
                    } finally {
                        EntityUtils.consumeQuietly(response.entity)
                    }
                }
            })
        } catch (e: Exception) {
            throw PostParseException(e)
        }
    }

    companion object {
        private val factory = SAXParserFactory.newInstance()
    }
}