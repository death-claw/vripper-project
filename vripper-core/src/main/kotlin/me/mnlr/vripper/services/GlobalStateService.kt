package me.mnlr.vripper.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.formatSI
import me.mnlr.vripper.model.GlobalState
import me.mnlr.vripper.repositories.ImageRepository

@Service
class GlobalStateService(
    private val downloadService: DownloadService,
    private val imageRepository: ImageRepository,
    private val eventBus: EventBus,
    private val vgAuthService: VGAuthService,
    private val downloadSpeedService: DownloadSpeedService
) {
    private var currentState: GlobalState = newValue()

    @Scheduled(fixedDelay = 200)
    private fun interval() {
        val newGlobalState = newValue()
        if (newGlobalState != currentState) {
            currentState = newGlobalState
            eventBus.publishEvent(Event(Event.Kind.DOWNLOAD_STATUS, currentState))
        }
    }

    private fun newValue(): GlobalState {
        return GlobalState(
            downloadService.runningCount(),
            downloadService.pendingCount(),
            imageRepository.countError(),
            vgAuthService.loggedUser,
            downloadSpeedService.currentValue.formatSI()
        )
    }

    fun get(): GlobalState {
        return currentState.copy()
    }
}
