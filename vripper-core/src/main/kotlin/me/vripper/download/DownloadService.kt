package me.vripper.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.entities.ImageEntity
import me.vripper.entities.LogEntryEntity
import me.vripper.entities.PostEntity
import me.vripper.entities.domain.Status
import me.vripper.event.ErrorCountEvent
import me.vripper.event.EventBus
import me.vripper.event.QueueStateEvent
import me.vripper.event.StoppedEvent
import me.vripper.host.Host
import me.vripper.model.ErrorCount
import me.vripper.model.QueueState
import me.vripper.services.DataTransaction
import me.vripper.services.RetryPolicyService
import me.vripper.services.SettingsService
import me.vripper.services.VGAuthService
import me.vripper.utilities.GLOBAL_EXECUTOR
import me.vripper.utilities.formatToString
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DownloadService(
    private val settingsService: SettingsService,
    private val dataTransaction: DataTransaction,
    private val retryPolicyService: RetryPolicyService,
    private val vgAuthService: VGAuthService,
    private val eventBus: EventBus
) {
    private val maxPoolSize: Int = 24
    private val log by me.vripper.delegate.LoggerDelegate()
    private val running: MutableMap<Byte, MutableList<ImageDownloadRunnable>> = mutableMapOf()
    private val pending: MutableMap<Byte, MutableList<ImageDownloadRunnable>> = mutableMapOf()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val coroutineScope = CoroutineScope(SupervisorJob())

    init {
        Thread.ofVirtual().name("Download Loop").unstarted(Runnable {
            val accepted: MutableList<ImageDownloadRunnable> = mutableListOf()
            val candidates: MutableList<ImageDownloadRunnable> = mutableListOf()
            while (!Thread.currentThread().isInterrupted) {
                lock.withLock {
                    candidates.addAll(getCandidates(candidateCount()))
                    candidates.forEach {
                        if (canRun(it.context.imageEntity.host)) {
                            accepted.add(it)
                            running[it.context.imageEntity.host]!!.add(it)
                            log.debug("${it.context.imageEntity.url} accepted to run")
                        }
                    }
                    accepted.forEach {
                        pending[it.context.imageEntity.host]?.remove(it)
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
        }).start()
    }

    fun stop(postIds: List<Long> = emptyList()) {
        if (postIds.isNotEmpty()) {
            stopInternal(postIds)
            coroutineScope.launch {
                eventBus.publishEvent(StoppedEvent(postIds))
            }
        } else {
            stopAll()
            coroutineScope.launch {
                eventBus.publishEvent(StoppedEvent(listOf(-1)))
            }
        }
    }

    fun restartAll(postEntityIds: List<PostEntity> = emptyList()) {
        if (postEntityIds.isNotEmpty()) {
            restart(postEntityIds.associateWith { dataTransaction.findByPostIdAndIsNotCompleted(it.postId) })
        } else {
            restart(dataTransaction.findAllPosts()
                .associateWith { dataTransaction.findByPostIdAndIsNotCompleted(it.postId) })
        }
    }

    private fun restart(posts: Map<PostEntity, List<ImageEntity>>) {
        lock.withLock {
            val toProcess = mutableMapOf<PostEntity, List<ImageEntity>>()

            for ((post, images) in posts) {
                if (images.isEmpty()) {
                    continue
                }
                if (isPending(post.postId)) {
                    continue
                }
                toProcess[post] = images
            }

            toProcess.forEach { (post, images) ->
                post.status = Status.PENDING
                images.forEach { image ->
                    with(image) {
                        this.status = Status.PENDING
                        this.downloaded = 0
                    }
                }
            }

            transaction {
                dataTransaction.updatePosts(
                    toProcess.keys.toList()
                )
                dataTransaction.updateImages(
                    toProcess.values.flatten()
                )
            }

            toProcess.keys.forEach {
                vgAuthService.leaveThanks(it)
            }


            toProcess.entries.forEach { (post, images) ->
                images.forEach { image ->
                    log.debug("Enqueuing a job for ${image.url}")
                    val imageDownloadRunnable = ImageDownloadRunnable(
                        image, post.rank, settingsService.settings
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

    private fun isPending(postId: Long): Boolean {
        return pending.values.flatten().any { it.context.imageEntity.postId == postId }
    }

    private fun isRunning(postId: Long): Boolean {
        return running.values.flatten().any { it.context.imageEntity.postId == postId }
    }

    private fun stopAll() {
        lock.withLock {
            pending.values.clear()
            running.values.flatten().forEach { obj: ImageDownloadRunnable -> obj.stop() }
            dataTransaction.findAllNonCompletedPostIds().forEach {
                dataTransaction.stopImagesByPostIdAndIsNotCompleted(it)
                dataTransaction.finishPost(it)
            }
        }
    }

    private fun stopInternal(postIds: List<Long>) {
        lock.withLock {
            for (postId in postIds) {
                pending.values.forEach { pending ->
                    pending.removeIf { it.context.imageEntity.postId == postId }
                }
                running.values.flatten()
                    .filter { p: ImageDownloadRunnable -> p.context.imageEntity.postId == postId }
                    .forEach { obj: ImageDownloadRunnable -> obj.stop() }
            }
            postIds.forEach {
                dataTransaction.stopImagesByPostIdAndIsNotCompleted(it)
                dataTransaction.finishPost(it)
            }
        }
    }

    private fun canRun(host: Byte): Boolean {
        val totalRunning = running.values.sumOf { it.size }
        return (running[host]!!.size < settingsService.settings.connectionSettings.maxConcurrentPerHost && if (settingsService.settings.connectionSettings.maxGlobalConcurrent == 0) totalRunning < maxPoolSize else totalRunning < settingsService.settings.connectionSettings.maxGlobalConcurrent)
    }

    private fun candidateCount(): Map<Byte, Int> {
        val map: MutableMap<Byte, Int> = mutableMapOf()
        Host.getHosts().values.forEach { host: Byte ->
            val imageDownloadRunnableList: List<ImageDownloadRunnable> = running.computeIfAbsent(
                host
            ) { mutableListOf() }
            val count: Int =
                settingsService.settings.connectionSettings.maxConcurrentPerHost - imageDownloadRunnableList.size
            log.debug("Download slots for $host: $count")
            map[host] = count
        }
        return map
    }

    private fun getCandidates(candidateCount: Map<Byte, Int>): List<ImageDownloadRunnable> {
        val hostIntegerMap: MutableMap<Byte, Int> = candidateCount.toMutableMap()
        val candidates: MutableList<ImageDownloadRunnable> = mutableListOf()
        hosts@ for (host in pending.keys) {

            val list: List<ImageDownloadRunnable> =
                pending[host]!!.sortedWith(Comparator.comparingInt<ImageDownloadRunnable> { it.postRank }
                    .thenComparingInt { it.context.imageEntity.index })

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
        return candidates
    }

    private fun scheduleForDownload(imageDownloadRunnable: ImageDownloadRunnable) {
        log.debug("Scheduling a job for ${imageDownloadRunnable.context.imageEntity.url}")
        CompletableFuture.runAsync({
            coroutineScope.launch {
                eventBus.publishEvent(QueueStateEvent(QueueState(runningCount(), pendingCount())))
            }
            Failsafe.with<Any, RetryPolicy<Any>>(retryPolicyService.buildRetryPolicyForDownload())
                .onFailure {
                    try {
                        dataTransaction.saveLog(
                            LogEntryEntity(
                                type = LogEntryEntity.Type.DOWNLOAD,
                                status = LogEntryEntity.Status.ERROR,
                                message = "Failed to download ${imageDownloadRunnable.context.imageEntity.url}\n ${it.failure.formatToString()}"
                            )
                        )
                    } catch (exp: Exception) {
                        log.error("Failed to save event", exp)
                    }
                    log.error(
                        "Failed to download ${imageDownloadRunnable.context.imageEntity.url} after ${it.attemptCount} tries",
                        it.failure
                    )
                    val image = imageDownloadRunnable.context.imageEntity
                    image.status = Status.ERROR
                    dataTransaction.updateImage(image)
                }.onComplete {
                    afterJobFinish(imageDownloadRunnable)
                    coroutineScope.launch {
                        eventBus.publishEvent(
                            QueueStateEvent(
                                QueueState(
                                    runningCount(), pendingCount()
                                )
                            )
                        )
                    }
                    coroutineScope.launch {
                        eventBus.publishEvent(ErrorCountEvent(ErrorCount(dataTransaction.countImagesInError())))
                    }
                    log.debug(
                        "Finished downloading ${imageDownloadRunnable.context.imageEntity.url}"
                    )
                }.run(imageDownloadRunnable::run)
        }, GLOBAL_EXECUTOR)
    }

    private fun afterJobFinish(imageDownloadRunnable: ImageDownloadRunnable) {
        lock.withLock {
            val image = imageDownloadRunnable.context.imageEntity
            running[image.host]!!.remove(imageDownloadRunnable)
            if (!isPending(image.postId) && !isRunning(
                    image.postId
                ) && !imageDownloadRunnable.context.stopped
            ) {
                dataTransaction.finishPost(image.postId, true)
            }
            condition.signal()
        }
    }

    private fun pendingCount(): Int {
        return pending.values.sumOf { it.size }
    }

    private fun runningCount(): Int {
        return running.values.sumOf { it.size }
    }
}