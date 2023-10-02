package me.mnlr.vripper.services

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.exception.VripperException
import me.mnlr.vripper.tasks.LeaveThanksRunnable
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.cookie.Cookie
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import reactor.core.Disposable
import java.util.concurrent.CompletableFuture

class VGAuthService(
    private val cm: HTTPService,
    private val settingsService: SettingsService,
    private val eventBusImpl: EventBus
) {
    private val log by LoggerDelegate()
    private val disposable: Disposable

    val context: HttpClientContext = HttpClientContext.create()
    var loggedUser = ""

    private var authenticated = false

    init {
        context.cookieStore = BasicCookieStore()
        disposable = eventBusImpl
            .flux()
            .filter { it.kind == Event.Kind.SETTINGS_UPDATE }
            .doOnNext {
                println(Thread.currentThread().name)
                authenticate()
            }.subscribe()
        authenticate()
    }

    private fun destroy() {
        disposable.dispose()
    }

    private fun authenticate() {
        authenticated = false
        if (!settingsService.settings.viperSettings.login) {
            log.debug("Authentication option is disabled")
            context.cookieStore.clear()
            loggedUser = ""
            eventBusImpl.publishEvent(Event(Event.Kind.VG_USER, loggedUser))
            return
        }
        val username = settingsService.settings.viperSettings.username
        val password = settingsService.settings.viperSettings.password
        if (username.isEmpty() || password.isEmpty()) {
            log.error("Cannot authenticate with ViperGirls credentials, username or password is empty")
            context.cookieStore.clear()
            loggedUser = ""
            eventBusImpl.publishEvent(Event(Event.Kind.VG_USER, loggedUser))
            return
        }
        val postAuth =
            cm.buildHttpPost(
                settingsService.settings.viperSettings.host + "/login.php?do=login",
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
            eventBusImpl.publishEvent(Event(Event.Kind.VG_USER, loggedUser))
            log.error(
                "Failed to authenticate user with " + settingsService.settings.viperSettings.host,
                e
            )
            return
        }
        postAuth.addHeader("Referer", settingsService.settings.viperSettings.host)
        postAuth.addHeader(
            "Host",
            settingsService.settings.viperSettings.host.replace("https://", "")
                .replace("http://", "")
        )
        val client = cm.client.build()
        try {
            client.execute(postAuth, context).use { response ->
                if (response.statusLine.statusCode / 100 != 2) {
                    throw VripperException(
                        String.format(
                            "Unexpected response code returned %s", response.statusLine.statusCode
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
                            settingsService.settings.viperSettings.host
                        )
                    )
                    return
                }
            }
        } catch (e: Exception) {
            context.cookieStore.clear()
            loggedUser = ""
            eventBusImpl.publishEvent(Event(Event.Kind.VG_USER, loggedUser))
            log.error(
                "Failed to authenticate user with " + settingsService.settings.viperSettings.host,
                e
            )
            return
        }
        authenticated = true
        loggedUser = username
        log.info(String.format("Authenticated: %s", username))
        eventBusImpl.publishEvent(Event(Event.Kind.VG_USER, loggedUser))
    }

    fun leaveThanks(post: PostDownloadState) {
        CompletableFuture.runAsync(LeaveThanksRunnable(post, authenticated, context))
    }
}