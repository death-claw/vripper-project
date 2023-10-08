package me.mnlr.vripper.tasks

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.LogEvent.Status.*
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.services.HTTPService
import me.mnlr.vripper.services.SettingsService
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.UnsupportedEncodingException

class LeaveThanksRunnable(
    private val post: Post,
    private val authenticated: Boolean,
    private val context: HttpClientContext
) : KoinComponent, Runnable {
    private val log by LoggerDelegate()
    private val cm: HTTPService by inject()
    private val settingsService: SettingsService by inject()
    private val eventRepository: LogEventRepository by inject()
    private val logEvent: LogEvent

    init {
        logEvent = eventRepository.save(
            LogEvent(
                type = LogEvent.Type.THANKS,
                status = PENDING,
                message = "Leaving thanks for $post.url"
            )
        )
    }

    override fun run() {
        try {
            eventRepository.update(logEvent.copy(status = PROCESSING))
            if (!settingsService.settings.viperSettings.login) {
                eventRepository.update(
                    logEvent.copy(
                        status = DONE,
                        message = "Will not send a like for ${post.url}\nAuthentication with ViperGirls option is disabled"
                    )
                )
                return
            }
            if (!settingsService.settings.viperSettings.thanks) {
                eventRepository.update(
                    logEvent.copy(
                        status = DONE,
                        message = "Will not send a like for ${post.url}\nLeave thanks option is disabled"
                    )
                )
                return
            }
            if (!authenticated) {
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR,
                        message = "Will not send a like for ${post.url}\nYou are not authenticated"
                    )
                )
                return
            }
            val postThanks: HttpPost = cm.buildHttpPost(
                "${settingsService.settings.viperSettings.host}/post_thanks.php", HttpClientContext.create()
            )
            val params: MutableList<NameValuePair> = ArrayList()
            params.add(BasicNameValuePair("do", "post_thanks_add"))
            params.add(BasicNameValuePair("using_ajax", "1"))
            params.add(BasicNameValuePair("p", post.postId))
            params.add(BasicNameValuePair("securitytoken", post.token))
            try {
                postThanks.entity = UrlEncodedFormEntity(params)
            } catch (e: UnsupportedEncodingException) {
                val error = "Request error for ${post.url}"
                log.error(error, e)
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR, message = """
                    $error
                    ${e.formatToString()}
                    """.trimIndent()
                    )
                )
                return
            }
            postThanks.addHeader("Referer", settingsService.settings.viperSettings.host)
            postThanks.addHeader(
                "Host", settingsService.settings.viperSettings.host.replace("https://", "").replace("http://", "")
            )
            val client: CloseableHttpClient = cm.client.build()
            client.execute(postThanks, context).use { response ->
                try {
                } finally {
                    EntityUtils.consumeQuietly(response.entity)
                }
            }
            eventRepository.update(logEvent.copy(status = DONE))
        } catch (e: Exception) {
            val error = "Failed to leave a thanks for $post"
            log.error(error, e)
            eventRepository.update(
                logEvent.copy(
                    status = ERROR, message = """
                $error
                ${e.formatToString()}
                """.trimIndent()
                )
            )
        }
    }
}