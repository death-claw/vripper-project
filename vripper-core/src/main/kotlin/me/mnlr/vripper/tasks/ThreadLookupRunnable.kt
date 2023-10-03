package me.mnlr.vripper.tasks

import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.PostDownloadRunnable
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.LogEvent.Status.*
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.services.DataTransaction
import me.mnlr.vripper.services.SettingsService
import me.mnlr.vripper.services.ThreadCacheService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.CompletableFuture

class ThreadLookupRunnable(private val threadId: String, private val settings: Settings) :
    KoinComponent, Runnable {
    private val log by LoggerDelegate()
    private val dataTransaction by inject<DataTransaction>()
    private val eventRepository by inject<LogEventRepository>()
    private val settingsService by inject<SettingsService>()
    private val threadCacheService by inject<ThreadCacheService>()
    private val appEndpointService by inject<AppEndpointService>()
    private val link: String =
        "${settingsService.settings.viperSettings.host}/threads/$threadId"
    private val logEvent: LogEvent

    init {
        logEvent = eventRepository.save(
            LogEvent(
                type = LogEvent.Type.THREAD,
                status = LogEvent.Status.PENDING,
                message = "Processing multi-post link $link"
            )
        )
    }

    override fun run() {
        try {
            eventRepository.update(logEvent.copy(status = PROCESSING))
            val threadLookupResult = threadCacheService[threadId]
            if (threadLookupResult.postItemList.isEmpty()) {
                val message = "Nothing found for $link"
                eventRepository.update(logEvent.copy(status = ERROR, message = message))
                return
            }
            if (dataTransaction.findThreadByThreadId(threadId).isEmpty) {
                dataTransaction.save(
                    Thread(
                        title = threadLookupResult.title,
                        link = link,
                        threadId = threadId,
                        total = threadLookupResult.postItemList.size
                    )
                )
                eventRepository.update(
                    logEvent.copy(
                        status = DONE, message = "New thread $link is added"
                    )
                )
                autostart(threadLookupResult)
            } else {
                log.info("Link $link is already loaded")
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR,
                        message = "$link has already been added to the queue"
                    )
                )
            }
        } catch (e: Exception) {
            val error = "Error when adding multi-post link $link"
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

    private fun autostart(lookupResult: ThreadItem) {
        if (lookupResult.postItemList.size <= settings.downloadSettings.autoQueueThreshold) {
            appEndpointService.threadRemove(listOf(lookupResult.threadId))
            lookupResult.postItemList.forEach {
                CompletableFuture.runAsync(PostDownloadRunnable(
                    it.threadId, it.postId
                ))
            }
        }
    }
}