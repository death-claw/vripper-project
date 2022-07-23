package me.mnlr.vripper

import org.springframework.stereotype.Service
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.exception.PostParseException
import me.mnlr.vripper.model.PostItem
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.repositories.ThreadRepository
import me.mnlr.vripper.services.*
import me.mnlr.vripper.tasks.LinkScanRunnable
import java.util.*
import java.util.regex.Pattern

@Service
class AppEndpointService(
    private val downloadService: DownloadService,
    private val dataTransaction: DataTransaction,
    private val threadRepository: ThreadRepository,
    private val threadCacheService: ThreadCacheService,
    private val eventRepository: LogEventRepository,
    private val threadPoolService: ThreadPoolService,
) {
    private val log by LoggerDelegate()

    @Synchronized
    fun scanLinks(postLinks: String) {
        if (postLinks.isBlank()) {
            log.warn("Nothing to scan")
            return
        }
        val urlList = postLinks.split(Pattern.compile("\\r?\\n")).dropLastWhile { it.isEmpty() }.map { it.trim() }
            .filter { it.isNotEmpty() }
        threadPoolService.generalExecutor.submit(LinkScanRunnable(urlList))
    }

    @Synchronized
    fun restartAll(posIds: List<String> = listOf()) {
        downloadService.restartAll(posIds)
    }

    @Synchronized
    fun download(posts: List<Pair<String, String>>) {
        downloadService.download(posts)
    }

    @Synchronized
    fun stopAll(postIdList: List<String>?) {
        downloadService.stopAll(postIdList)
    }

    @Synchronized
    fun remove(postIdList: List<String>) {
        downloadService.stopAll(postIdList)
        dataTransaction.removeAll(postIdList)
    }

    @Synchronized
    fun clearCompleted(): List<String> {
        return dataTransaction.clearCompleted()
    }

    @Synchronized
    @Throws(PostParseException::class)
    fun grab(threadId: String): List<PostItem> {
        return try {
            val thread = threadRepository.findByThreadId(threadId).orElseThrow {
                PostParseException(
                    String.format(
                        "Unable to find links for threadId = %s", threadId
                    )
                )
            }
            val threadLookupResult = threadCacheService[thread.threadId]
            threadLookupResult.postItemList.ifEmpty {
                log.error(
                    String.format(
                        "Failed to get links for threadId = %s", threadId
                    )
                )
                throw PostParseException(
                    String.format(
                        "Failed to get links for threadId = %s", threadId
                    )
                )
            }
        } catch (e: Exception) {
            throw PostParseException(
                String.format(
                    "Failed to get links for threadId = %s, %s", threadId, e.message
                )
            )
        }
    }

    @Synchronized
    fun threadRemove(threadIdList: List<String>) {
        threadIdList.forEach {
            dataTransaction.removeThread(it)
        }
    }

    @Synchronized
    fun threadClear() {
        dataTransaction.clearQueueLinks()
    }

    fun logClear() {
        eventRepository.deleteAll()
    }
}