package me.mnlr.vripper.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.SettingsUpdateEvent
import me.mnlr.vripper.event.VGUserLoginEvent
import me.mnlr.vripper.exception.VripperException
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.tasks.LeaveThanksRunnable
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.cookie.Cookie
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.NameValuePair
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicNameValuePair
import java.util.concurrent.CompletableFuture

class VGAuthService(
    private val cm: HTTPService,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) {
    private val log by LoggerDelegate()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val context: HttpClientContext = HttpClientContext.create()
    var loggedUser = ""

    private var authenticated = false

    fun init() {
        context.cookieStore = BasicCookieStore()
        coroutineScope.launch {
            eventBus.subscribe<SettingsUpdateEvent> {
                authenticate(it.settings)
            }
        }
        authenticate(settingsService.settings)
    }

    private fun authenticate(settings: Settings) {
        authenticated = false
        if (!settings.viperSettings.login) {
            log.debug("Authentication option is disabled")
            context.cookieStore.clear()
            loggedUser = ""
                eventBus.publishEvent(VGUserLoginEvent(loggedUser))

            return
        }
        val username = settings.viperSettings.username
        val password = settings.viperSettings.password
        if (username.isEmpty() || password.isEmpty()) {
            log.error("Cannot authenticate with ViperGirls credentials, username or password is empty")
            context.cookieStore.clear()
            loggedUser = ""

                eventBus.publishEvent(VGUserLoginEvent(loggedUser))

            return
        }
        val postAuth =
            cm.buildHttpPost(
                settings.viperSettings.host + "/login.php?do=login",
                context
            )
        val params: MutableList<NameValuePair> = ArrayList()
        params.add(BasicNameValuePair("vb_login_username", username))
        params.add(BasicNameValuePair("cookieuser", "1"))
        params.add(BasicNameValuePair("do", "login"))
        params.add(BasicNameValuePair("vb_login_md5password", password))
        try {
            postAuth.entity = UrlEncodedFormEntity(params)
        } catch (e: Exception) {
            context.cookieStore.clear()
            loggedUser = ""

            eventBus.publishEvent(VGUserLoginEvent(loggedUser))

            log.error(
                "Failed to authenticate user with " + settings.viperSettings.host,
                e
            )
            return
        }
        postAuth.addHeader("Referer", settings.viperSettings.host)
        postAuth.addHeader(
            "Host",
            settings.viperSettings.host.replace("https://", "")
                .replace("http://", "")
        )
        val client = cm.clientBuilder.build()
        try {
            client.execute(postAuth, context).use { response ->
                if (response.code / 100 != 2) {
                    throw VripperException(
                        String.format(
                            "Unexpected response code returned %s", response.code
                        )
                    )
                }
                val responseBody = EntityUtils.toString(response.entity)
                log.debug(
                    String.format(
                        "Authentication with ViperGirls response body:%n%s",
                        responseBody
                    )
                )
                EntityUtils.consumeQuietly(response.entity)
                if (context.cookieStore.cookies.stream()
                        .map { obj: Cookie -> obj.name }
                        .noneMatch { e: String -> e == "vg_userid" }
                ) {
                    log.error(
                        String.format(
                            "Failed to authenticate user with %s, missing vg_userid cookie",
                            settings.viperSettings.host
                        )
                    )
                    return
                }
            }
        } catch (e: Exception) {
            context.cookieStore.clear()
            loggedUser = ""

            eventBus.publishEvent(VGUserLoginEvent(loggedUser))

            log.error(
                "Failed to authenticate user with " + settings.viperSettings.host,
                e
            )
            return
        }
        authenticated = true
        loggedUser = username
        log.info(String.format("Authenticated: %s", username))

        eventBus.publishEvent(VGUserLoginEvent(loggedUser))

    }

    fun leaveThanks(post: Post) {
        CompletableFuture.runAsync(LeaveThanksRunnable(post, authenticated, context))
    }
}