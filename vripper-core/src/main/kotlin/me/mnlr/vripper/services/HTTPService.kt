package me.mnlr.vripper.services

import kotlinx.coroutines.*
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.SettingsUpdateEvent
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpHead
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpUriRequest
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.pool.PoolConcurrencyPolicy
import org.apache.hc.core5.pool.PoolReusePolicy
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import java.net.URI

class HTTPService(
    val eventBus: EventBus,
    settingsService: SettingsService
) {

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var pcm: PoolingAsyncClientConnectionManager
    private lateinit var rc: RequestConfig
    private lateinit var cc: ConnectionConfig
    lateinit var clientBuilder: HttpAsyncClientBuilder
    private var connectionTimeout: Long = settingsService.settings.connectionSettings.timeout

    fun init() {
        buildRequestConfig()
        buildConnectionConfig()
        buildConnectionPool()
        buildClientBuilder()
        coroutineScope.launch {
            pcm.closeIdle(TimeValue.ofSeconds(60))
            delay(15000)
        }
        coroutineScope.launch {
            eventBus
                .subscribe<SettingsUpdateEvent> {
                    if (connectionTimeout != it.settings.connectionSettings.timeout) {
                        connectionTimeout = it.settings.connectionSettings.timeout
                        buildRequestConfig()
                        buildConnectionConfig()
                        pcm.close()
                        buildConnectionPool()
                        buildClientBuilder()
                    }
                }
        }
    }

    private fun buildConnectionPool() {
        pcm = PoolingAsyncClientConnectionManagerBuilder.create()
            .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
            .setConnPoolPolicy(PoolReusePolicy.LIFO)
            .setDefaultConnectionConfig(cc)
            .setMaxConnTotal(Int.MAX_VALUE)
            .setMaxConnPerRoute(Int.MAX_VALUE)
            .build()
    }

    private fun buildRequestConfig() {
        rc = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(connectionTimeout))
            .setCookieSpec(StandardCookieSpec.RELAXED)
            .build()
    }

    private fun buildConnectionConfig() {
        cc = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(connectionTimeout))
            .setSocketTimeout(Timeout.ofSeconds(connectionTimeout))
            .setTimeToLive(TimeValue.ofMinutes(10))
            .build()
    }

    private fun buildClientBuilder() {
        clientBuilder = HttpAsyncClients.custom()
            .setConnectionManager(pcm)
            .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
            .disableAutomaticRetries()
            .setDefaultRequestConfig(rc)
    }

    fun buildHttpGet(url: String, context: HttpClientContext): HttpGet {
        val httpGet = HttpGet(url.replace(" ", "+"))
        httpGet.addHeader("User-Agent", USER_AGENT)
        addToContext(context, httpGet)
        return httpGet
    }

    fun buildHttpHead(url: String, context: HttpClientContext): HttpHead {
        SimpleRequestBuilder.head(url)
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

    fun addToContext(context: HttpClientContext, request: HttpUriRequest) {
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
        val requests: MutableList<HttpUriRequest> = mutableListOf()

        companion object {
            const val CONTEXT_ATTRIBUTES = "CONTEXT_ATTRIBUTES"
        }
    }
}