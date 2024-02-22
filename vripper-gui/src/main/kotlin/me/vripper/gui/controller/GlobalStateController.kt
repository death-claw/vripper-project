package me.vripper.gui.controller

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.time.sample
import me.vripper.event.*
import me.vripper.event.EventBus
import me.vripper.gui.model.GlobalStateModel
import me.vripper.services.VGAuthService
import me.vripper.utilities.formatSI
import tornadofx.*
import java.time.Duration

class GlobalStateController : Controller() {

    private val eventBus: EventBus by di()
    private val vgAuthService: VGAuthService by di()

    var globalState: GlobalStateModel =
        GlobalStateModel(0, 0, 0, vgAuthService.loggedUser, 0L.formatSI(), false)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    fun init() {
        coroutineScope.launch {
            eventBus.events.filterIsInstance(DownloadSpeedEvent::class).collect {
                runLater {
                    globalState.downloadSpeed = it.downloadSpeed.speed.formatSI()
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(VGUserLoginEvent::class).collect {
                runLater {
                    globalState.loggedUser = it.username
                }
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(QueueStateEvent::class).collect {
                runLater {
                    globalState.apply {
                        running = it.queueState.running
                        remaining = it.queueState.remaining
                    }
                }
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(ErrorCountEvent::class).collect {
                runLater {
                    globalState.error = it.errorCount.count
                }
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(LoadingTasks::class).sample(Duration.ofMillis(500))
                .collect {
                    runLater {
                        globalState.loading = it.loading
                    }
                }
        }
    }
}