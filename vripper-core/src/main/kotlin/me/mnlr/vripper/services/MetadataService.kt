package me.mnlr.vripper.services

import org.springframework.stereotype.Service
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.tasks.MetadataRunnable
import java.util.function.Consumer

@Service
class MetadataService(private val threadPoolService: ThreadPoolService) {
    private val fetchingMetadata: MutableMap<String, MetadataRunnable> = mutableMapOf()

    @Synchronized
    fun startFetchingMetadata(postDownloadState: PostDownloadState) {
        val runnable = MetadataRunnable(postDownloadState)
        threadPoolService.generalExecutor.submit(runnable)
        fetchingMetadata[postDownloadState.postId] = runnable
    }

    @Synchronized
    fun stopFetchingMetadata(postIds: List<String>) {
        val stopping: MutableList<MetadataRunnable> = ArrayList()
        for ((key, value) in fetchingMetadata) {
            if (postIds.contains(key)) {
                stopping.add(value)
                value.stop()
            }
        }
        while (stopping.isNotEmpty()) {
            stopping.removeIf { it.finished }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        postIds.forEach(Consumer { key: String -> fetchingMetadata.remove(key) })
    }
}