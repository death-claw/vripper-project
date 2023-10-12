package me.mnlr.vripper.download

import kotlinx.coroutines.*
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.event.ErrorCountEvent
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.QueueStateEvent
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.host.Host
import me.mnlr.vripper.model.ErrorCount
import me.mnlr.vripper.model.QueueState
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.services.DataTransaction
import me.mnlr.vripper.services.DownloadSpeedService
import me.mnlr.vripper.services.RetryPolicyService
import me.mnlr.vripper.services.SettingsService
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors
import kotlin.concurrent.withLock

class DownloadService(
    private val settingsService: SettingsService,
    private val dataTransaction: DataTransaction,
    private val retryPolicyService: RetryPolicyService,
    private val eventRepository: LogEventRepository,
    private val downloadSpeedService: DownloadSpeedService,
    private val eventBus: EventBus
) {
    private val maxPoolSize: Int = 12
    private val log by LoggerDelegate()

    // Class fields
    private val running: MutableMap<String, MutableList<ImageDownloadRunnable>> = mutableMapOf()
    private val pending: MutableMap<String, MutableList<ImageDownloadRunnable>> = mutableMapOf()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init() {
        coroutineScope.launch(Dispatchers.Default) {
            val accepted: MutableList<ImageDownloadRunnable> = mutableListOf()
            val candidates: MutableList<ImageDownloadRunnable> = mutableListOf()
            while (isActive) {
                lock.withLock {
                    candidates.addAll(getCandidates(candidateCount()))
                    candidates.forEach {
                        if (canRun(it.context.image.host)) {
                            accepted.add(it)
                            running[it.context.image.host]!!.add(it)
                            log.debug("${it.context.image.url} accepted to run")
                        }
                    }
                    accepted.forEach {
                        pending[it.context.image.host]?.remove(it)
                        scheduleForDownload(it)
                    }
                    accepted.clear()
                    candidates.clear()
                    try {
                        condition.await()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        }
    }

    fun destroy() {
        stop(dataTransaction.findAllPosts().map { it.postId })
    }

    fun stopAll(postIds: List<String>?) {
        if (postIds != null) {
            stop(postIds)
        } else {
            stop(dataTransaction.findAllPosts().map(Post::postId))
        }
    }

    fun restartAll(postIds: List<String> = listOf()) {
        if (postIds.isNotEmpty()) {
            restart(postIds)
        } else {
            restart(dataTransaction.findAllPosts().map(Post::postId))
        }
    }

    private fun restart(postIds: List<String>) {
        lock.withLock {
            val data: MutableMap<Post, Collection<Image>> = mutableMapOf()
            for (postId in postIds) {
                if (isPending(postId)) {
                    log.warn("Cannot restart, jobs are currently running for post id $postIds")
                    continue
                }
                val images: List<Image> =
                    dataTransaction.findByPostIdAndIsNotCompleted(postId)
                if (images.isEmpty()) {
                    continue
                }
                val post: Post =
                    dataTransaction.findPostsByPostId(postId).orElseThrow()
                log.debug("Restarting ${images.size} jobs for post id $postIds")
                post.status = Status.PENDING
                dataTransaction.update(post)
                data[post] = images
            }

            for ((_, images) in data) {
                for (image in images) {
                    log.debug("Enqueuing a job for ${image.url}")
                    with(image) {
                        this.status = Status.PENDING
                        this.current = 0
                    }
                    dataTransaction.update(image)
                    val imageDownloadRunnable = ImageDownloadRunnable(
                        image.id!!, settingsService.settings
                    )
                    pending.computeIfAbsent(
                        image.host
                    ) { mutableListOf() }
                    pending[image.host]!!.add(imageDownloadRunnable)
                }
            }
            condition.signal()
        }
    }

    private fun isPending(postId: String): Boolean {
        lock.withLock {
            return pending.values.flatten().any { it.context.image.postId == postId }
        }
    }

    private fun isRunning(postId: String): Boolean {
        lock.withLock {
            return running.values.flatten().any { it.context.image.postId == postId }
        }
    }

    private fun stop(postIds: List<String>) {
        lock.withLock {
            for (postId in postIds) {
                val post: Post =
                    dataTransaction.findPostsByPostId(postId).orElseThrow()
                pending.values.forEach { pending ->
                    pending.removeIf { it.context.image.postId == postId }
                }
                running.values.flatten()
                    .filter { p: ImageDownloadRunnable -> p.context.image.postId == postId }
                    .forEach { obj: ImageDownloadRunnable -> obj.stop() }
                dataTransaction.stopImagesByPostIdAndIsNotCompleted(postId)
                dataTransaction.finishPost(post)
            }
        }
    }

    private fun canRun(host: String): Boolean {
        val totalRunning = running.values.sumOf { it.size }
        return (running[host]!!.size < settingsService.settings.connectionSettings.maxThreads && if (settingsService.settings.connectionSettings.maxTotalThreads == 0) totalRunning < maxPoolSize else totalRunning < settingsService.settings.connectionSettings.maxTotalThreads)
    }

    private fun candidateCount(): Map<String, Int> {
        val map: MutableMap<String, Int> = mutableMapOf()
        Host.getHosts().forEach { host: String ->
            val imageDownloadRunnableList: List<ImageDownloadRunnable> = running.computeIfAbsent(
                host
            ) { mutableListOf() }
            val count: Int =
                settingsService.settings.connectionSettings.maxThreads - imageDownloadRunnableList.size
            log.debug("Download slots for $host: $count")
            map[host] = count
        }
        return map
    }

    private fun getCandidates(candidateCount: Map<String, Int>): List<ImageDownloadRunnable> {
        val hostIntegerMap: MutableMap<String, Int> = candidateCount.toMutableMap()
        val candidates: MutableList<ImageDownloadRunnable> = mutableListOf()
        hosts@ for (host in pending.keys) {
            val list: List<ImageDownloadRunnable> =
                pending[host]!!.sortedWith(Comparator.comparingInt<ImageDownloadRunnable> { it.context.post.rank }
                    .thenComparingInt { it.context.image.index })
            for (imageDownloadRunnable in list) {
                val count = hostIntegerMap[host] ?: 0
                if (count > 0) {
                    candidates.add(imageDownloadRunnable)
                    hostIntegerMap[host] = count - 1
                } else {
                    continue@hosts
                }
            }
        }
        if (log.isDebugEnabled) {
            val collect: Map<String, List<ImageDownloadRunnable>> =
                candidates.stream().collect(Collectors.groupingBy { it.context.image.host })
            collect.forEach {
                log.debug(
                    "Candidate download for ${it.key} ${it.value.size}/${candidateCount[it.key]}"
                )
            }
        }
        return candidates.sortedWith(Comparator.comparing { v: ImageDownloadRunnable -> v.context.post.rank })
    }

    private fun scheduleForDownload(imageDownloadRunnable: ImageDownloadRunnable) {
        log.debug("Scheduling a job for ${imageDownloadRunnable.context.image.url}")
        coroutineScope.launch {
            launch {
                eventBus.publishEvent(QueueStateEvent(QueueState(runningCount(), pendingCount())))
            }
            try {
                Failsafe.with<Any, RetryPolicy<Any>>(retryPolicyService.buildRetryPolicyForDownload())
                    .onFailure {
                        try {
                            eventRepository.save(
                                LogEvent(
                                    type = LogEvent.Type.DOWNLOAD,
                                    status = LogEvent.Status.ERROR,
                                    message = "Failed to download ${imageDownloadRunnable.context.image.url}\n ${it.failure.formatToString()}"
                                )
                            )
                        } catch (exp: Exception) {
                            log.error("Failed to save event", exp)
                        }
                        log.error(
                            "Failed to download ${imageDownloadRunnable.context.image.url} after ${it.attemptCount} tries",
                            it.failure
                        )
                        val image = imageDownloadRunnable.context.image
                        image.status = Status.ERROR
                        dataTransaction.update(image)
                        launch {
                            eventBus.publishEvent(ErrorCountEvent(ErrorCount(dataTransaction.countImagesInError())))
                        }
                    }.onComplete {
                        afterJobFinish(imageDownloadRunnable)
                        launch {
                            eventBus.publishEvent(
                                QueueStateEvent(
                                    QueueState(
                                        runningCount(),
                                        pendingCount()
                                    )
                                )
                            )
                        }
                        log.debug(
                            "Finished downloading ${imageDownloadRunnable.context.image.url}"
                        )
                    }.run(imageDownloadRunnable::run)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun afterJobFinish(imageDownloadRunnable: ImageDownloadRunnable) {
        lock.withLock {
            val image = imageDownloadRunnable.context.image
            running[image.host]!!.remove(imageDownloadRunnable)
            if (!isPending(image.postId) && !isRunning(
                    image.postId
                )
            ) {
                dataTransaction.finishPost(imageDownloadRunnable.context.post)
            }
            condition.signal()
        }
    }

    fun pendingCount(): Int {
        return pending.values.sumOf { it.size }
    }

    fun runningCount(): Int {
        return running.values.sumOf { it.size }
    }
}