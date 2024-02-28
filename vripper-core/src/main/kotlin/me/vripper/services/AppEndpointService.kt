package me.vripper.services

import me.vripper.download.DownloadService
import me.vripper.entities.LogEntry
import me.vripper.exception.PostParseException
import me.vripper.model.PostItem
import me.vripper.model.ThreadPostId
import me.vripper.services.*
import me.vripper.tasks.AddPostRunnable
import me.vripper.tasks.ThreadLookupRunnable
import me.vripper.utilities.GLOBAL_EXECUTOR
import me.vripper.utilities.PathUtils
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import kotlin.io.path.Path
import kotlin.io.path.exists

class AppEndpointService(
    private val downloadService: DownloadService,
    private val dataTransaction: DataTransaction,
    private val threadCacheService: ThreadCacheService,
    private val settingsService: SettingsService
) {
    private val log by me.vripper.delegate.LoggerDelegate()

    @Synchronized
    fun scanLinks(postLinks: String) {
        if (postLinks.isBlank()) {
            log.warn("Nothing to scan")
            return
        }
        val urlList = postLinks.split(Pattern.compile("\\r?\\n")).dropLastWhile { it.isEmpty() }.map { it.trim() }
            .filter { it.isNotEmpty() }
        for (link in urlList) {
            log.debug("Starting to process thread: $link")
            if (!link.startsWith(settingsService.settings.viperSettings.host)) {
                continue
            }
            var threadId: Long
            var postId: Long?
            val m = Pattern.compile(
                Pattern.quote(settingsService.settings.viperSettings.host) + "/threads/(\\d+)((.*p=)(\\d+))?"
            ).matcher(link)
            if (m.find()) {
                threadId = m.group(1).toLong()
                postId = m.group(4)?.toLong()
                if (postId == null) {
                    CompletableFuture.runAsync(
                        ThreadLookupRunnable(
                            threadId, settingsService.settings
                        ),
                        GLOBAL_EXECUTOR
                    )
                } else {
                    CompletableFuture.runAsync(
                        AddPostRunnable(
                            listOf(ThreadPostId(threadId, postId))
                        ),
                        GLOBAL_EXECUTOR
                    )
                }
            } else {
                log.error("Cannot retrieve thread id from URL $link")
                dataTransaction.saveLog(
                    LogEntry(
                        type = LogEntry.Type.SCAN,
                        status = LogEntry.Status.ERROR,
                        message = "Invalid link $link, link is missing the threadId"
                    )
                )
                continue
            }
        }
    }

    @Synchronized
    fun restartAll(posIds: List<Long> = listOf()) {
        downloadService.restartAll(posIds.map { dataTransaction.findPostByPostId(it) }.filter { it.isPresent }
            .map { it.get() })
    }

    @Synchronized
    fun download(posts: List<ThreadPostId>) {
        CompletableFuture.runAsync(
            AddPostRunnable(posts), GLOBAL_EXECUTOR
        )
    }

    @Synchronized
    fun stopAll(postIdList: List<Long> = emptyList()) {
        downloadService.stop(postIdList)
    }

    @Synchronized
    fun remove(postIdList: List<Long>) {
        downloadService.stop(postIdList)
        dataTransaction.removeAll(postIdList)
    }

    @Synchronized
    fun clearCompleted(): List<Long> {
        return dataTransaction.clearCompleted()
    }

    @Synchronized
    @Throws(PostParseException::class)
    fun grab(threadId: Long): List<PostItem> {
        return try {
            val thread = dataTransaction.findThreadByThreadId(threadId).orElseThrow {
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
    fun threadRemove(threadIdList: List<Long>) {
        threadIdList.forEach {
            dataTransaction.removeThread(it)
        }
    }

    @Synchronized
    fun threadClear() {
        dataTransaction.clearQueueLinks()
    }

    fun logClear() {
        dataTransaction.deleteAllLogs()
    }

    fun rename(postId: Long, newName: String) {
        CompletableFuture.runAsync({
            synchronized(postId.toString().intern()) {
                dataTransaction.findPostByPostId(postId).ifPresent { post ->
                    if (Path(post.downloadDirectory, post.folderName).exists()) {
                        PathUtils.rename(
                            dataTransaction.findImagesByPostId(postId),
                            post.downloadDirectory,
                            post.folderName,
                            newName
                        )
                    }
                    post.folderName = PathUtils.sanitize(newName)
                    dataTransaction.updatePost(post)
                }
            }
        }, GLOBAL_EXECUTOR)
    }
}