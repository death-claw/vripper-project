package me.mnlr.vripper.gui.clipboard

import javafx.scene.input.Clipboard
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.SettingsUpdateEvent
import me.mnlr.vripper.model.Settings

class ClipboardService(
    private val appEndpointService: AppEndpointService,
    private val eventBus: EventBus
) {
    private var current: String? = null
    private var coroutineScope = CoroutineScope(Dispatchers.JavaFx)
    private var pollJob: Job? = null

    fun init() {
        coroutineScope.launch {
            eventBus.subscribe<SettingsUpdateEvent> {
                run(it.settings)
            }
        }
    }

    fun run(settings: Settings) {
        pollJob?.cancel()
        if (settings.clipboardSettings.enable) {
            pollJob = coroutineScope.launch {
                while (isActive) {
                    poll()
                    delay(settings.clipboardSettings.pollingRate.toLong())
                }
            }
        } else {
            current = null
        }
    }

    private fun poll() {
        val clipboard = Clipboard.getSystemClipboard()
        if (clipboard.hasString()) {
            val value: String? = clipboard.string
            if (!value.isNullOrBlank() && value != this.current) {
                this.current = value
                appEndpointService.scanLinks(value)
            }
        }
    }

    fun destroy() {
        coroutineScope.cancel()
    }
}