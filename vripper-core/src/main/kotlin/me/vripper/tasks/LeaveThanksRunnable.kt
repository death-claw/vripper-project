package me.vripper.tasks

import me.vripper.entities.LogEntryEntity
import me.vripper.entities.LogEntryEntity.Status.ERROR
import me.vripper.entities.PostEntity
import me.vripper.services.DataTransaction
import me.vripper.services.HTTPService
import me.vripper.services.SettingsService
import me.vripper.utilities.Tasks
import me.vripper.utilities.formatToString
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LeaveThanksRunnable(
    private val postEntity: PostEntity,
    private val authenticated: Boolean,
    private val context: HttpClientContext
) : KoinComponent, Runnable {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val cm: HTTPService by inject()
    private val settingsService: SettingsService by inject()
    private val dataTransaction: DataTransaction by inject()

    override fun run() {
        try {
            Tasks.increment()
            if (!settingsService.settings.viperSettings.login || !authenticated) {
                return
            }
            if (!settingsService.settings.viperSettings.thanks) {
                return
            }

            val postThanks =
                HttpPost("${settingsService.settings.viperSettings.host}/post_thanks.php").also {
                    it.entity = UrlEncodedFormEntity(
                        listOf(
                            BasicNameValuePair("do", "post_thanks_add"),
                            BasicNameValuePair("using_ajax", "1"),
                            BasicNameValuePair("p", postEntity.postId.toString()),
                            BasicNameValuePair("securitytoken", postEntity.token)
                        )
                    )
                    it.addHeader("Referer", settingsService.settings.viperSettings.host)
                    it.addHeader(
                        "Host",
                        settingsService.settings.viperSettings.host.replace("https://", "")
                            .replace("http://", "")
                    )
                }
            cm.client.execute(postThanks, context) { }
        } catch (e: Exception) {
            val error = "Failed to leave a thanks for $postEntity"
            log.error(error, e)
            dataTransaction.saveLog(
                LogEntryEntity(
                    type = LogEntryEntity.Type.THANKS, status = ERROR, message = """
                $error
                ${e.formatToString()}
                """.trimIndent()
                )
            )
        } finally {
            Tasks.decrement()
        }
    }
}