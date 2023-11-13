package me.vripper.tasks

import me.vripper.entities.LogEntry
import me.vripper.entities.LogEntry.Status.*
import me.vripper.entities.Thread
import me.vripper.model.Settings
import me.vripper.model.ThreadItem
import me.vripper.model.ThreadPostId
import me.vripper.services.AppEndpointService
import me.vripper.services.DataTransaction
import me.vripper.services.SettingsService
import me.vripper.services.ThreadCacheService
import me.vripper.utilities.GLOBAL_EXECUTOR
import me.vripper.utilities.Tasks
import me.vripper.utilities.formatToString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.CompletableFuture

class ThreadLookupRunnable(private val threadId: Long, private val settings: Settings) :
    KoinComponent, Runnable {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val dataTransaction by inject<DataTransaction>()
    private val settingsService by inject<SettingsService>()
    private val threadCacheService by inject<ThreadCacheService>()
    private val appEndpointService by inject<AppEndpointService>()
    private val link: String = "${settingsService.settings.viperSettings.host}/threads/$threadId"
    private val logEntry: LogEntry

    init {
        logEntry = dataTransaction.saveLog(
            LogEntry(
                type = LogEntry.Type.THREAD,
                status = LogEntry.Status.PENDING,
                message = "Processing multi-post link $link"
            )
        )
    }

    override fun run() {
        try {
            Tasks.increment()
            dataTransaction.updateLog(logEntry.copy(status = PROCESSING))
            if (dataTransaction.findThreadByThreadId(threadId).isEmpty) {
                val threadLookupResult = threadCacheService[threadId]
                if (threadLookupResult.postItemList.isEmpty()) {
                    val message = "Nothing found for $link"
                    dataTransaction.updateLog(logEntry.copy(status = ERROR, message = message))
                    return
                }
                dataTransaction.save(
                    Thread(
                        title = threadLookupResult.title,
                        link = link,
                        threadId = threadId,
                        total = threadLookupResult.postItemList.size
                    )
                )
                dataTransaction.updateLog(
                    logEntry.copy(
                        status = DONE,
                        message = "Thread scan completed, found ${threadLookupResult.postItemList.size} posts"
                    )
                )
                autostart(threadLookupResult)
            } else {
                log.info("Link $link is already loaded")
                dataTransaction.updateLog(
                    logEntry.copy(
                        status = DONE, message = "$link has already been scanned"
                    )
                )
            }
        } catch (e: Exception) {
            val error = "Error when adding multi-post link $link"
            log.error(error, e)
            dataTransaction.updateLog(
                logEntry.copy(
                    status = ERROR, message = """
                $error
                ${e.formatToString()}
                """.trimIndent()
                )
            )
        } finally {
            Tasks.decrement()
        }
    }

    private fun autostart(lookupResult: ThreadItem) {
        if (lookupResult.postItemList.size <= settings.downloadSettings.autoQueueThreshold) {
            appEndpointService.threadRemove(listOf(lookupResult.threadId))
            CompletableFuture.runAsync(
                AddPostRunnable(
                    lookupResult.postItemList.map { ThreadPostId(it.threadId, it.postId) }
                ),
                GLOBAL_EXECUTOR
            )
        }
    }
}