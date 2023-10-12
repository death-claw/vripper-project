package me.mnlr.vripper.services

import kotlinx.coroutines.*
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.SettingsUpdateEvent
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.AbstractExecutionAwareRequest
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import java.net.URI
import java.util.concurrent.TimeUnit

class HTTPService(
    val eventBus: EventBus,
    settingsService: SettingsService
) {

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var pcm: PoolingHttpClientConnectionManager
    private lateinit var rc: RequestConfig
    private var connectionTimeout: Int = settingsService.settings.connectionSettings.timeout

    fun init() {
        buildRequestConfig()
        buildConnectionPool()
        coroutineScope.launch {
            pcm.closeIdleConnections(60, TimeUnit.SECONDS)
            delay(15000)
        }
        coroutineScope.launch {
            eventBus
                .subscribe<SettingsUpdateEvent> {
                    if (connectionTimeout != it.settings.connectionSettings.timeout) {
                        connectionTimeout = it.settings.connectionSettings.timeout
                        buildRequestConfig()
                    }
                }
        }
    }

    private fun buildConnectionPool() {
        pcm = PoolingHttpClientConnectionManager()
        pcm.maxTotal = Int.MAX_VALUE
        pcm.defaultMaxPerRoute = Int.MAX_VALUE
    }

    private fun buildRequestConfig() {
        rc = RequestConfig.custom()
            .setConnectionRequestTimeout(connectionTimeout * 1000)
            .setConnectTimeout(connectionTimeout * 1000)
            .setSocketTimeout(connectionTimeout * 1000)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build()
    }

    val client: HttpClientBuilder
        get() = HttpClients.custom()
            .setConnectionManager(pcm)
            .setRedirectStrategy(LaxRedirectStrategy())
            .disableAutomaticRetries()
            .setDefaultRequestConfig(rc)

    fun buildHttpGet(url: String, context: HttpClientContext): HttpGet {
        val httpGet = HttpGet(url.replace(" ", "+"))
        httpGet.addHeader("User-Agent", USER_AGENT)
        addToContext(context, httpGet)
        return httpGet
    }

    fun buildHttpHead(url: String, context: HttpClientContext): HttpHead {
        val httpHead = HttpHead(url.replace(" ", "+"))
        httpHead.addHeader("User-Agent", USER_AGENT)
        addToContext(context, httpHead)
        return httpHead
    }

    fun buildHttpPost(url: String, context: HttpClientContext): HttpPost {
        val httpPost = HttpPost(url.replace(" ", "+"))
        httpPost.addHeader("User-Agent", USER_AGENT)
        addToContext(context, httpPost)
        return httpPost
    }

    fun buildHttpGet(uri: URI, context: HttpClientContext): HttpGet {
        val httpGet = HttpGet(uri)
        httpGet.addHeader("User-Agent", USER_AGENT)
        addToContext(context, httpGet)
        return httpGet
    }

    fun addToContext(context: HttpClientContext, request: AbstractExecutionAwareRequest) {
        val contextAttributes =
            context.getAttribute(
                ContextAttributes.CONTEXT_ATTRIBUTES,
                ContextAttributes::class.java
            )
        if (contextAttributes != null) {
            synchronized(contextAttributes.requests) {
                contextAttributes.requests.add(request)
            }
        }
    }

    class ContextAttributes {
        val requests: MutableList<AbstractExecutionAwareRequest> = mutableListOf()

        companion object {
            const val CONTEXT_ATTRIBUTES = "CONTEXT_ATTRIBUTES"
        }
    }
}