package me.mnlr.vripper.download

import jakarta.annotation.PostConstruct
import net.jodah.failsafe.Failsafe
import net.jodah.failsafe.RetryPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.formatToString
import me.mnlr.vripper.host.Host
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.services.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors
import kotlin.concurrent.withLock

@Service
class DownloadService(
    @param:Value("\${download.pool-size}") private val maxPoolSize: Int,
    private val settingsService: SettingsService,
    private val dataTransaction: DataTransaction,
    private val metadataService: MetadataService,
    private val hosts: List<Host>,
    private val eventRepository: LogEventRepository,
    private val retryPolicyService: RetryPolicyService,
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val imageRepository: ImageRepository,
    private val threadPoolService: ThreadPoolService,
) {
    private val log by LoggerDelegate()

    // Class fields
    private val executor: ExecutorService = Executors.newFixedThreadPool(maxPoolSize)
    private val running: MutableMap<Host, MutableList<ImageDownloadRunnable>> = mutableMapOf()
    private val pending: MutableMap<Host, MutableList<ImageDownloadRunnable>> = mutableMapOf()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val pollThread: Thread

    init {
        pollThread = Thread(
            {
                val accepted: MutableList<ImageDownloadRunnable> = mutableListOf()
                val candidates: MutableList<ImageDownloadRunnable> = mutableListOf()
                while (!Thread.interrupted()) {
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
                            schedule(it)
                        }
                        accepted.clear()
                        candidates.clear()
                        condition.await()
                    }
                }
            }, "Download scheduler thread"
        )
    }

    @PostConstruct
    fun init() {
        pollThread.start()
    }

    fun destroy() {
        log.info("Shutting down ExecutionService")
        pollThread.interrupt()
        executor.shutdown()
        stop(postDownloadStateRepository.findAll().map { it.postId })
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.warn("Some jobs are still running!, forcing shutdown")
            executor.shutdownNow()
        }
    }

    fun stopAll(postIds: List<String>?) {
        if (postIds != null) {
            stop(postIds)
        } else {
            stop(postDownloadStateRepository.findAll().map(PostDownloadState::postId))
        }
    }

    fun restartAll(postIds: List<String> = listOf()) {
        if (postIds.isNotEmpty()) {
            restart(postIds)
        } else {
            restart(postDownloadStateRepository.findAll().map(PostDownloadState::postId))
        }
    }

    private fun restart(postIds: List<String>) {
        lock.withLock {
            val data: MutableMap<PostDownloadState, Collection<ImageDownloadState>> = mutableMapOf()
            for (postId in postIds) {
                if (isPending(postId)) {
                    log.warn("Cannot restart, jobs are currently running for post id $postIds")
                    continue
                }
                val imageDownloadStates: List<ImageDownloadState> =
                    imageRepository.findByPostIdAndIsNotCompleted(postId)
                if (imageDownloadStates.isEmpty()) {
                    continue
                }
                val postDownloadState: PostDownloadState =
                    postDownloadStateRepository.findByPostId(postId).orElseThrow()
                log.debug("Restarting ${imageDownloadStates.size} jobs for post id $postIds")
                postDownloadState.status = Status.PENDING
                dataTransaction.update(postDownloadState)
                data[postDownloadState] = imageDownloadStates
            }

            for ((postDownloadState, imageDownloadStates) in data) {
                postDownloadState.status = Status.PENDING
                dataTransaction.update(postDownloadState)
                for (imageDownloadState in imageDownloadStates) {
                    log.debug("Enqueuing a job for ${imageDownloadState.url}")
                    with(imageDownloadState) {
                        this.status = Status.STOPPED
                        this.current = 0
                    }
                    dataTransaction.update(imageDownloadState)
                    val imageDownloadRunnable = ImageDownloadRunnable(
                        imageDownloadState.id!!, settingsService.settings
                    )
                    pending.computeIfAbsent(
                        imageDownloadState.host
                    ) { mutableListOf() }
                    pending[imageDownloadState.host]!!.add(imageDownloadRunnable)
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
                val postDownloadState: PostDownloadState =
                    postDownloadStateRepository.findByPostId(postId).orElseThrow()
                pending.values.forEach { pending ->
                    pending.removeIf { it.context.image.postId == postId }
                }
                running.values.flatten()
                    .filter { p: ImageDownloadRunnable -> p.context.image.postId == postId }
                    .forEach { obj: ImageDownloadRunnable -> obj.stop() }
                dataTransaction.stopImagesByPostIdAndIsNotCompleted(postId)
                dataTransaction.finishPost(postDownloadState)
            }
            metadataService.stopFetchingMetadata(postIds)
        }
    }

    private fun canRun(host: Host): Boolean {
        val totalRunning = running.values.sumOf { it.size }
        return (running[host]!!.size < settingsService.settings.connectionSettings.maxThreads && if (settingsService.settings.connectionSettings.maxTotalThreads == 0) totalRunning < maxPoolSize else totalRunning < settingsService.settings.connectionSettings.maxTotalThreads)
    }

    private fun candidateCount(): Map<Host, Int> {
        val map: MutableMap<Host, Int> = mutableMapOf()
        hosts.forEach { h: Host ->
            val imageDownloadRunnableList: List<ImageDownloadRunnable> = running.computeIfAbsent(
                h
            ) { mutableListOf() }
            val count: Int = settingsService.settings.connectionSettings.maxThreads - imageDownloadRunnableList.size
            log.debug("Download slots for ${h.host}: $count")
            map[h] = count
        }
        return map
    }

    private fun getCandidates(candidateCount: Map<Host, Int>): List<ImageDownloadRunnable> {
        val hostIntegerMap: MutableMap<Host, Int> = candidateCount.toMutableMap()
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
            val collect: Map<Host, List<ImageDownloadRunnable>> =
                candidates.stream().collect(Collectors.groupingBy { it.context.image.host })
            collect.forEach {
                log.debug(
                    "Candidate download for ${it.key.host} ${it.value.size}/${candidateCount[it.key]}"
                )
            }
        }
        return candidates.sortedWith(Comparator.comparing { v: ImageDownloadRunnable -> v.context.post.rank })
    }

    private fun schedule(imageDownloadRunnable: ImageDownloadRunnable) {
        log.debug("Scheduling a job for ${imageDownloadRunnable.context.image.url}")
        executor.execute {
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
                    }.onComplete {

                        afterJobFinish(imageDownloadRunnable)
                        log.debug(
                            "Finished downloading ${imageDownloadRunnable.context.image.url}"
                        )
                    }.run(imageDownloadRunnable::run)
            } catch (ignored: Exception) {
            }
        }
    }

    fun afterJobFinish(imageDownloadRunnable: ImageDownloadRunnable) {
        lock.withLock {
            running[imageDownloadRunnable.context.image.host]!!.remove(imageDownloadRunnable)
            if (!isPending(imageDownloadRunnable.context.image.postId) && !isRunning(
                    imageDownloadRunnable.context.image.postId
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

    fun download(posts: List<Pair<String, String>>) {
        for (post in posts) {
            threadPoolService.generalExecutor.submit(
                PostDownloadRunnable(
                    post.first, post.second
                )
            )
        }
    }
}