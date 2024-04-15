package me.vripper.services

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.time.sample
import me.vripper.download.DownloadService
import me.vripper.entities.LogEntryEntity
import me.vripper.entities.PostEntity
import me.vripper.event.*
import me.vripper.exception.PostParseException
import me.vripper.model.*
import me.vripper.tasks.AddPostRunnable
import me.vripper.tasks.ThreadLookupRunnable
import me.vripper.utilities.ApplicationProperties
import me.vripper.utilities.GLOBAL_EXECUTOR
import me.vripper.utilities.PathUtils
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.jvm.optionals.getOrNull

class AppEndpointService(
    private val downloadService: DownloadService,
    private val dataTransaction: DataTransaction,
    private val threadCacheService: ThreadCacheService,
    private val settingsService: SettingsService,
    private val vgAuthService: VGAuthService,
) : IAppEndpointService {
    private val log by me.vripper.delegate.LoggerDelegate()

    private val lock = ReentrantLock()

    override suspend fun scanLinks(postLinks: String) {
        lock.withLock {
            if (postLinks.isBlank()) {
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
                            ), GLOBAL_EXECUTOR
                        )
                    } else {
                        CompletableFuture.runAsync(
                            AddPostRunnable(
                                listOf(ThreadPostId(threadId, postId))
                            ), GLOBAL_EXECUTOR
                        )
                    }
                } else {
                    log.error("Cannot retrieve thread id from URL $link")
                    dataTransaction.saveLog(
                        LogEntryEntity(
                            type = LogEntryEntity.Type.SCAN,
                            status = LogEntryEntity.Status.ERROR,
                            message = "Invalid link $link, link is missing the threadId"
                        )
                    )
                    continue
                }
            }
        }
    }

    override suspend fun restartAll(posIds: List<Long>) {
        lock.withLock {
            downloadService.restartAll(posIds.map { dataTransaction.findPostByPostId(it) }.filter { it.isPresent }
                .map { it.get() })
        }
    }

    override suspend fun download(posts: List<ThreadPostId>) {
        CompletableFuture.runAsync(
            AddPostRunnable(posts), GLOBAL_EXECUTOR
        )
    }

    override suspend fun stopAll(postIdList: List<Long>) {
        lock.withLock {
            downloadService.stop(postIdList)
        }
    }

    override suspend fun remove(postIdList: List<Long>) {
        lock.withLock {
            downloadService.stop(postIdList)
            dataTransaction.removeAll(postIdList)
        }
    }

    override suspend fun clearCompleted(): List<Long> {
        lock.withLock {
            return dataTransaction.clearCompleted()
        }
    }

    override suspend fun grab(threadId: Long): List<PostSelection> {
        lock.withLock {
            return try {
                val thread = dataTransaction.findThreadByThreadId(threadId).orElseThrow {
                    PostParseException(
                        String.format(
                            "Unable to find links for threadId = %s", threadId
                        )
                    )
                }
                val threadLookupResult = threadCacheService[thread.threadId]
                threadLookupResult.postItemList.map { postItem ->
                    PostSelection(
                        postItem.threadId,
                        postItem.threadTitle,
                        postItem.postId,
                        postItem.number,
                        postItem.title,
                        postItem.imageCount,
                        postItem.url,
                        postItem.hosts.joinToString(", ") { "${it.first} (${it.second})" },
                        postItem.forum,
                        postItem.imageItemList.map { it.thumbLink })
                }.ifEmpty {
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
    }

    override suspend fun threadRemove(threadIdList: List<Long>) {
        lock.withLock {
            threadIdList.forEach {
                dataTransaction.removeThread(it)
            }
        }
    }

    override suspend fun threadClear() {
        lock.withLock {
            dataTransaction.clearQueueLinks()
        }
    }

    override suspend fun logClear() {
        lock.withLock {
            dataTransaction.deleteAllLogs()
        }
    }

    override suspend fun renameToFirst(postIds: List<Long>) {
        postIds.forEach { postId ->
            dataTransaction
                .findMetadataByPostId(postId)
                .map { it.data.resolvedNames }
                .filter { it.isNotEmpty() }
                .getOrNull()?.let { rename(postId, it.first()) }
        }
    }

    override suspend fun rename(postId: Long, newName: String) {
        CompletableFuture.runAsync({
            synchronized(postId.toString().intern()) {
                dataTransaction.findPostByPostId(postId).ifPresent { post ->
                    if (Path(post.downloadDirectory, post.folderName).exists()) {
                        PathUtils.rename(
                            dataTransaction.findImagesByPostId(postId), post.downloadDirectory, post.folderName, newName
                        )
                    }
                    post.folderName = PathUtils.sanitize(newName)
                    dataTransaction.updatePost(post)
                }
            }
        }, GLOBAL_EXECUTOR)
    }

    override fun onNewPosts(): Flow<Post> =
        EventBus.events.filterIsInstance(PostCreateEvent::class).map { it.postEntities.map(::mapper) }
            .flatMapConcat { it.asFlow() }


    override fun onUpdatePosts() =
        EventBus.events.filterIsInstance(PostUpdateEvent::class).map { it.postEntities.map(::mapper) }
            .flatMapConcat { it.asFlow() }


    override fun onDeletePosts() =
        EventBus.events.filterIsInstance(PostDeleteEvent::class).flatMapConcat { it.postIds.asFlow() }


    override fun onUpdateMetadata() =
        EventBus.events.filterIsInstance(MetadataUpdateEvent::class).map { it.metadataEntity }


    override suspend fun findAllPosts(): List<Post> {
        return dataTransaction.findAllPosts().map(::mapper)
    }

    private fun mapper(postEntity: PostEntity): Post {
        val metadata: Metadata? = dataTransaction.findMetadataByPostId(postEntity.postId).orElse(null)
        val images = dataTransaction.findImagesByPostId(postEntity.postId)
        return Post(
            postEntity.id,
            postEntity.postTitle,
            postEntity.threadTitle,
            postEntity.forum,
            postEntity.url,
            postEntity.token,
            postEntity.postId,
            postEntity.threadId,
            postEntity.total,
            postEntity.hosts,
            postEntity.downloadDirectory,
            postEntity.addedOn,
            postEntity.folderName,
            postEntity.status,
            postEntity.done,
            postEntity.rank,
            postEntity.size,
            postEntity.downloaded,
            images.take(4).map { it.thumbUrl },
            metadata?.data?.postedBy ?: "",
            metadata?.data?.resolvedNames ?: emptyList(),
        )
    }

    override suspend fun findPost(postId: Long): Post {
        return mapper(dataTransaction.findPostByPostId(postId).orElseThrow())
    }

    override suspend fun findImagesByPostId(postId: Long): List<Image> {
        return dataTransaction.findImagesByPostId(postId)
    }

    override fun onUpdateImages(postId: Long): Flow<Image> = EventBus.events.filterIsInstance(ImageEvent::class).map {
        it.imageEntities.filter { imageEntity: Image -> imageEntity.postId == postId }
    }.filter { it.isNotEmpty() }.flatMapConcat { it.asFlow() }


    override fun onStopped(): Flow<Long> =
        EventBus.events.filterIsInstance(StoppedEvent::class).flatMapConcat { it.postIds.asFlow() }


    override suspend fun findAllLogs(): List<LogEntry> {
        return dataTransaction.findAllLogs()
    }

    override fun onNewLog() = EventBus.events.filterIsInstance(LogCreateEvent::class).map { it.logEntryEntity }


    override fun onUpdateLog() = EventBus.events.filterIsInstance(LogUpdateEvent::class).map {
        it.logEntryEntity
    }


    override fun onDeleteLogs(): Flow<Long> = EventBus.events.filterIsInstance(LogDeleteEvent::class).flatMapConcat {
        it.deleted.asFlow()
    }


    override fun onNewThread(): Flow<Thread> =
        EventBus.events.filterIsInstance(ThreadCreateEvent::class).map { it.threadEntity }


    override fun onUpdateThread(): Flow<Thread> =
        EventBus.events.filterIsInstance(ThreadUpdateEvent::class).map { it.threadEntity }


    override fun onDeleteThread(): Flow<Long> =
        EventBus.events.filterIsInstance(ThreadDeleteEvent::class).map { it.threadId }


    override fun onClearThreads(): Flow<Unit> = EventBus.events.filterIsInstance(ThreadClearEvent::class).map { }


    override suspend fun findAllThreads(): List<Thread> {
        return dataTransaction.findAllThreads()
    }

    override fun onDownloadSpeed(): Flow<Long> =
        EventBus.events.filterIsInstance(DownloadSpeedEvent::class).map { it.downloadSpeed.speed }

    override fun onVGUserUpdate(): Flow<String> =
        EventBus.events.filterIsInstance(VGUserLoginEvent::class).map { it.username }

    override fun onQueueStateUpdate(): Flow<QueueState> =
        EventBus.events.filterIsInstance(QueueStateEvent::class).map { it.queueState }

    override fun onErrorCountUpdate(): Flow<Int> =
        EventBus.events.filterIsInstance(ErrorCountEvent::class).map { it.errorCount.count }

    override fun onTasksRunning(): Flow<Boolean> =
        EventBus.events.filterIsInstance(LoadingTasks::class).sample(Duration.ofMillis(500)).map { it.loading }

    override suspend fun getSettings(): Settings = settingsService.settings

    override suspend fun saveSettings(settings: Settings) = settingsService.newSettings(settings)

    override suspend fun getProxies(): List<String> = settingsService.getProxies()

    override fun onUpdateSettings(): Flow<Settings> =
        EventBus.events.filterIsInstance(SettingsUpdateEvent::class).map { it.settings }

    override suspend fun loggedInUser(): String = vgAuthService.loggedUser

    override suspend fun getVersion(): String = ApplicationProperties.VERSION

}