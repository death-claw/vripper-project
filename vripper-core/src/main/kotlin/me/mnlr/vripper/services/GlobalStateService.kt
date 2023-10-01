package me.mnlr.vripper.services

import kotlinx.coroutines.*
import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.formatSI
import me.mnlr.vripper.model.GlobalState
import me.mnlr.vripper.repositories.ImageRepository
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicLong

class GlobalStateService(
    private val downloadService: DownloadService,
    private val vgAuthService: VGAuthService,
    private val eventBus: EventBus,
    private val imageRepository: ImageRepository
) {

    private var currentState: GlobalState = newValue()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val bytesCount = AtomicLong(0)
    private var currentValue = 0L

    fun init() {
        coroutineScope.launch {
            while (isActive) {
                val newValue = bytesCount.getAndSet(0)
                currentValue = newValue
                delay(1000)
            }
        }

        coroutineScope.launch {
            while (isActive) {
                val newGlobalState = newValue()
                if (newGlobalState != currentState) {
                    currentState = newGlobalState
                    eventBus.publishEvent(Event(Event.Kind.DOWNLOAD_STATUS, currentState))
                }
                delay(200)
            }
        }
    }

    fun reportDownloadedBytes(count: Long) {
        this.bytesCount.addAndGet(count)
    }

    private fun newValue(): GlobalState {
        return GlobalState(
            downloadService.runningCount(),
            downloadService.pendingCount(),
            transaction { imageRepository.countError() } ,
            vgAuthService.loggedUser,
            currentValue.formatSI()
        )
    }

    fun get(): GlobalState {
        return currentState.copy()
    }
}
