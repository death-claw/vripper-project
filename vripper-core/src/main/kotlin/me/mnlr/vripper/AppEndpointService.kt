package me.mnlr.vripper

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.download.PostDownloadRunnable
import me.mnlr.vripper.exception.PostParseException
import me.mnlr.vripper.model.PostItem
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.repositories.ThreadRepository
import me.mnlr.vripper.services.*
import me.mnlr.vripper.tasks.ThreadLookupRunnable
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Pattern

@Service
class AppEndpointService(
    private val downloadService: DownloadService,
    private val dataTransaction: DataTransaction,
    private val threadRepository: ThreadRepository,
    private val threadCacheService: ThreadCacheService,
    private val eventRepository: LogEventRepository,
    private val asyncTaskRunnerService: AsyncTaskRunnerService,
    private val settingsService: SettingsService
) {
    private val log by LoggerDelegate()

    @Synchronized
    fun scanLinks(postLinks: String) {
        if (postLinks.isBlank()) {
            log.warn("Nothing to scan")
            return
        }
        val urlList = postLinks.split(Pattern.compile("\\r?\\n")).dropLastWhile { it.isEmpty() }
            .map { it.trim() }.filter { it.isNotEmpty() }
        for (link in urlList) {
            log.debug("Starting to process thread: $link")
            if (!link.startsWith(settingsService.settings.viperSettings.host)) {
                continue
            }
            var threadId: String
            var postId: String?
            val m = Pattern.compile(
                Pattern.quote(settingsService.settings.viperSettings.host) + "/threads/(\\d+)((.*p=)(\\d+))?"
            ).matcher(link)
            if (m.find()) {
                threadId = m.group(1)
                postId = m.group(4)
                if (postId == null) {
                    asyncTaskRunnerService.sink.emitNext(
                        ThreadLookupRunnable(
                            threadId, settingsService.settings
                        )
                    ) { _, _ -> true }
                } else {
                    asyncTaskRunnerService.sink.emitNext(
                        PostDownloadRunnable(
                            threadId, postId
                        )
                    ) { _, _ -> true }
                }
            } else {
                log.error("Cannot retrieve thread id from URL $link")
                continue
            }
        }
    }

    @Synchronized
    fun restartAll(posIds: List<String> = listOf()) {
        downloadService.restartAll(posIds)
    }

    @Synchronized
    fun download(posts: List<Pair<String, String>>) {
        for (post in posts) {
            asyncTaskRunnerService.sink.emitNext(
                PostDownloadRunnable(
                    post.first, post.second
                )
            ) { _, _ -> true }
        }
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