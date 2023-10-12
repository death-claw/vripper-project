package me.mnlr.vripper.gui.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.mnlr.vripper.event.*
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.gui.model.GlobalStateModel
import tornadofx.*

class GlobalStateController : Controller() {

    private val eventBus: EventBus by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var globalState: GlobalStateModel = GlobalStateModel(0, 0, 0, "", "0 B")

    fun init() {
        coroutineScope.launch {
            eventBus.subscribe<DownloadSpeedEvent> {
                globalState.apply {
                    downloadSpeed = it.downloadSpeed.speed
                }
            }
        }

        coroutineScope.launch {
            eventBus.subscribe<VGUserLoginEvent> {
                val user = it.username
                globalState.apply {
                    loggedUser = user
                }
            }
        }

        coroutineScope.launch {
            eventBus.subscribe<QueueStateEvent> {
                globalState.apply {
                    running = it.queueState.running
                    remaining = it.queueState.remaining
                }
            }
        }

        coroutineScope.launch {
            eventBus.subscribe<ErrorCountEvent> {
                globalState.apply {
                    error = it.errorCount.count
                }
            }
        }
    }
}