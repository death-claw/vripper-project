package me.mnlr.vripper.tasks

import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.LogEvent.Status.*
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.model.ThreadInput
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.repositories.ThreadRepository
import me.mnlr.vripper.services.DataTransaction
import me.mnlr.vripper.services.SettingsService
import me.mnlr.vripper.services.ThreadCacheService

class ThreadLookupRunnable(private val threadInput: ThreadInput, private val settings: Settings) : Runnable {
    private val log by LoggerDelegate()
    private val dataTransaction = SpringContext.getBean(DataTransaction::class.java)
    private val eventRepository = SpringContext.getBean(LogEventRepository::class.java)
    private val threadCacheService = SpringContext.getBean(
        ThreadCacheService::class.java
    )
    private val threadRepository = SpringContext.getBean(
        ThreadRepository::class.java
    )
    private val downloadService = SpringContext.getBean(DownloadService::class.java)
    private val logEvent: LogEvent

    init {
        logEvent = eventRepository.save(
            LogEvent(
                type = LogEvent.Type.THREAD,
                status = LogEvent.Status.PENDING,
                message = "Processing multi-post link ${threadInput.link}"
            )
        )
    }

    override fun run() {
        try {
            eventRepository.update(logEvent.copy(status = PROCESSING))
            val threadLookupResult = threadCacheService[threadInput.threadId]
            if (threadLookupResult.postItemList.isEmpty()) {
                val message = "Nothing found for " + threadInput.link
                eventRepository.update(logEvent.copy(status = ERROR, message = message))
                return
            }
//            if (threadItemList.size == 1) {
//                threadPoolService.generalExecutor.submit(
//                    SinglePostRunnable(
//                        threadItemList[0].postId, threadItemList[0].threadId
//                    )
//                )
//                eventRepository.update(
//                    logEvent.copy(
//                        status = DONE,
//                        message = "Thread ${thread.link} is added to the queue"
//                    )
//                )
//            } else {
            if (threadRepository.findByThreadId(threadInput.threadId).isEmpty) {
                dataTransaction.save(
                    Thread(
                        link = threadInput.link,
                        threadId = threadInput.threadId,
                        total = threadLookupResult.postItemList.size
                    )
                )
                eventRepository.update(
                    logEvent.copy(
                        status = DONE, message = "New thread ${threadInput.link} is added"
                    )
                )
                autostart(threadLookupResult)
            } else {
                log.info("Link ${threadInput.link} is already loaded")
                eventRepository.update(
                    logEvent.copy(
                        status = ERROR, message = "${threadInput.link} has already been added to the queue"
                    )
                )
            }
//            }
        } catch (e: Exception) {
            val error = "Error when adding multi-post link ${threadInput.link}"
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
        if(lookupResult.postItemList.size <= settings.downloadSettings.autoQueueThreshold) {
            downloadService.download(lookupResult.postItemList.map { Pair(it.threadId, it.postId) })
        }
    }
}