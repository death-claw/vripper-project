package me.mnlr.vripper.gui.controller

import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.gui.model.GlobalStateModel
import me.mnlr.vripper.model.GlobalState
import me.mnlr.vripper.services.GlobalStateService
import tornadofx.*

class GlobalStateController : Controller() {

    private val globalStateService: GlobalStateService by di()
    private val eventBus: EventBus by di()

    var globalState: GlobalStateModel = GlobalStateModel(0, 0, 0, "", "")

    init {
        val currentState = globalStateService.get()
        globalState.apply {
            running = currentState.running
            remaining = currentState.remaining
            error = currentState.error
            loggedUser = currentState.loggedUser
            downloadSpeed = currentState.downloadSpeed
        }

        eventBus.flux().doOnNext {
            if (it!!.kind == Event.Kind.DOWNLOAD_STATUS
            ) {
                val newState = it.data as GlobalState
                globalState.apply {
                    running = newState.running
                    remaining = newState.remaining
                    error = newState.error
                }
            }
        }.doOnNext {
            if (it!!.kind == Event.Kind.VG_USER
            ) {
                val user = it.data as String
                globalState.apply {
                    loggedUser = user
                }
            }
        }.subscribe()
    }
}