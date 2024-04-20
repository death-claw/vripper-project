package me.vripper.tasks

import me.vripper.entities.LogEntryEntity
import me.vripper.entities.LogEntryEntity.Status.ERROR
import me.vripper.entities.ThreadEntity
import me.vripper.model.Settings
import me.vripper.model.ThreadPostId
import me.vripper.services.DataTransaction
import me.vripper.services.SettingsService
import me.vripper.services.ThreadCacheService
import me.vripper.utilities.GLOBAL_EXECUTOR
import me.vripper.utilities.Tasks
import me.vripper.utilities.formatToString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.CompletableFuture

class ThreadLookupRunnable(private val threadId: Long, private val settings: Settings) : KoinComponent, Runnable {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val dataTransaction by inject<DataTransaction>()
    private val settingsService by inject<SettingsService>()
    private val threadCacheService by inject<ThreadCacheService>()
    private val link: String = "${settingsService.settings.viperSettings.host}/threads/$threadId"

    override fun run() {
        try {
            Tasks.increment()
            if (dataTransaction.findThreadByThreadId(threadId).isEmpty) {
                val threadLookupResult = threadCacheService[threadId]
                if (threadLookupResult.error.isNotBlank()) {
                    dataTransaction.saveLog(
                        LogEntryEntity(
                            type = LogEntryEntity.Type.THREAD,
                            status = LogEntryEntity.Status.ERROR,
                            message = "Error loading $link: ${threadLookupResult.error}"
                        )
                    )
                    return
                }
                if (threadLookupResult.postItemList.isEmpty()) {
                    dataTransaction.saveLog(
                        LogEntryEntity(
                            type = LogEntryEntity.Type.THREAD,
                            status = LogEntryEntity.Status.ERROR,
                            message = "Nothing found for $link"
                        )
                    )
                    return
                }

                if (threadLookupResult.postItemList.size <= settings.downloadSettings.autoQueueThreshold) {
                    CompletableFuture.runAsync(AddPostRunnable(threadLookupResult.postItemList.map {
                        ThreadPostId(
                            it.threadId, it.postId
                        )
                    }), GLOBAL_EXECUTOR)
                } else {
                    try {
                        dataTransaction.save(
                            ThreadEntity(
                                title = threadLookupResult.title,
                                link = link,
                                threadId = threadId,
                                total = threadLookupResult.postItemList.size
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            val error = "Error when processing $link"
            log.error(error, e)
            dataTransaction.saveLog(
                LogEntryEntity(
                    type = LogEntryEntity.Type.THREAD, status = ERROR, message = """
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