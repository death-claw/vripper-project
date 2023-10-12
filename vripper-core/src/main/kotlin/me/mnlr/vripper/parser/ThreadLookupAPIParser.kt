package me.mnlr.vripper.parser

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.exception.PostParseException
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.services.HTTPService
import me.mnlr.vripper.services.RetryPolicyService
import me.mnlr.vripper.services.SettingsService
import me.mnlr.vripper.services.VGAuthService
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.function.CheckedSupplier
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.net.URIBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedInputStream
import java.net.URISyntaxException
import java.util.*
import javax.xml.parsers.SAXParserFactory

class ThreadLookupAPIParser(private val threadId: String) : KoinComponent {
    private val log by LoggerDelegate()
    private val cm: HTTPService by inject()
    private val retryPolicyService: RetryPolicyService by inject()
    private val vgAuthService: VGAuthService by inject()
    private val settingsService: SettingsService by inject()

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
                val connection: HttpClient = cm.clientBuilder.build()
                (connection.execute(
                    httpGet, vgAuthService.context
                ) as CloseableHttpResponse).use { response ->
                    try {
                        if (response.code / 100 != 2) {
                            throw DownloadException("Unexpected response code '${response.code}' for $httpGet")
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