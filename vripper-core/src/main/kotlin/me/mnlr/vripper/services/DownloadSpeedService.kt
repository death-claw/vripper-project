package me.mnlr.vripper.services

import kotlinx.coroutines.*
import me.mnlr.vripper.event.DownloadSpeedEvent
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.QueueStateEvent
import me.mnlr.vripper.formatSI
import me.mnlr.vripper.model.DownloadSpeed
import java.util.concurrent.atomic.AtomicLong

class DownloadSpeedService(
    private val eventBus: EventBus,
) {

    companion object {
        const val DOWNLOAD_POLL_RATE = 2500
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val bytesCount = AtomicLong(0)
    private var job: Job? = null

    fun init() {
        coroutineScope.launch {
            eventBus.subscribe<QueueStateEvent> {
                if (it.queueState.running + it.queueState.remaining > 0) {
                    if (job == null || job?.isActive == false) {
                        job = coroutineScope.launch {
                            eventBus.publishEvent(DownloadSpeedEvent(DownloadSpeed(0L.formatSI())))
                            while (isActive) {
                                delay(DOWNLOAD_POLL_RATE.toLong())
                                val newValue = bytesCount.getAndSet(0)

                                eventBus.publishEvent(DownloadSpeedEvent(DownloadSpeed(((newValue * 1000) / DOWNLOAD_POLL_RATE).formatSI())))

                            }
                        }
                    }
                } else {
                    job?.cancel()
                    coroutineScope.launch {
                        delay(DOWNLOAD_POLL_RATE + 500L)
                        eventBus.publishEvent(DownloadSpeedEvent(DownloadSpeed(0L.formatSI())))
                    }
                }
            }
        }
    }

    fun reportDownloadedBytes(count: Long) {
        bytesCount.addAndGet(count)
    }
}
