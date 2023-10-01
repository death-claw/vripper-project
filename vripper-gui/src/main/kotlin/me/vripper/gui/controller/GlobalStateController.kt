package me.vripper.gui.controller

import me.vripper.event.*
import me.vripper.gui.model.GlobalStateModel
import me.vripper.services.VGAuthService
import me.vripper.utilities.formatSI
import tornadofx.Controller
import tornadofx.runLater
import java.time.Duration

class GlobalStateController : Controller() {

    private val eventBus: EventBus by di()
    private val vgAuthService: VGAuthService by di()

    var globalState: GlobalStateModel = GlobalStateModel(0, 0, 0, vgAuthService.loggedUser, 0L.formatSI(), false)

    fun init() {
        eventBus.events.ofType(DownloadSpeedEvent::class.java).subscribe {
            runLater {
                globalState.downloadSpeed = it.downloadSpeed.speed.formatSI()
            }
        }

        eventBus.events.ofType(VGUserLoginEvent::class.java).subscribe {
            runLater {
                globalState.loggedUser = it.username
            }
        }

        eventBus.events.ofType(QueueStateEvent::class.java).subscribe {
            runLater {
                globalState.apply {
                    running = it.queueState.running
                    remaining = it.queueState.remaining
                }
            }
        }

        eventBus.events.ofType(ErrorCountEvent::class.java).subscribe {
            runLater {
                globalState.error = it.errorCount.count
            }
        }

        eventBus.events.ofType(LoadingTasks::class.java).sample(Duration.ofMillis(500)).subscribe {
            runLater {
                globalState.loading = it.loading
            }
        }
    }
}