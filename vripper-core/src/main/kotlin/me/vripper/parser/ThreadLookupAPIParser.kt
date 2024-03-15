package me.vripper.parser

import me.vripper.entities.LogEntryEntity
import me.vripper.exception.DownloadException
import me.vripper.exception.PostParseException
import me.vripper.services.*
import me.vripper.utilities.Tasks
import me.vripper.utilities.formatToString
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.function.CheckedSupplier
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.net.URIBuilder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.BufferedInputStream
import java.util.*
import javax.xml.parsers.SAXParserFactory

class ThreadLookupAPIParser(private val threadId: Long) : KoinComponent {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val cm: HTTPService by inject()
    private val retryPolicyService: RetryPolicyService by inject()
    private val vgAuthService: VGAuthService by inject()
    private val settingsService: SettingsService by inject()
    private val dataTransaction: DataTransaction by inject()

    @Throws(PostParseException::class)
    fun parse(): ThreadItem {
        log.debug("Parsing thread $threadId")
        val httpGet =
            HttpGet(URIBuilder(settingsService.settings.viperSettings.host + "/vr.php").also {
                it.setParameter(
                    "t",
                    threadId.toString()
                )
            }.build())
        val threadLookupAPIResponseHandler = ThreadLookupAPIResponseHandler()
        log.debug("Requesting {}", httpGet)
        Tasks.increment()
        return try {
            Failsafe.with(retryPolicyService.buildGenericRetryPolicy<Any>()).onFailure {
                log.error(
                    "parsing failed for thread $threadId",
                    it.failure
                )
                dataTransaction.saveLog(
                    LogEntryEntity(
                        type = LogEntryEntity.Type.THREAD, status = LogEntryEntity.Status.ERROR, message = """
                    Failed to process thread $threadId
                    ${it.failure.formatToString()}
                    """.trimIndent()
                    )
                )
            }.get(CheckedSupplier {
                cm.client.execute(
                    httpGet, vgAuthService.context
                ) { response ->
                    if (response.code / 100 != 2) {
                        throw DownloadException("Unexpected response code '${response.code}' for $httpGet")
                    }
                    factory.newSAXParser().parse(
                        BufferedInputStream(response.entity.content),
                        threadLookupAPIResponseHandler
                    )
                    threadLookupAPIResponseHandler.result
                }
            })
        } catch (e: Exception) {
            throw PostParseException(e)
        } finally {
            Tasks.decrement()
        }
    }

    companion object {
        private val factory = SAXParserFactory.newInstance()
    }
}