package me.vripper.gui.clipboard

import javafx.scene.input.Clipboard
import kotlinx.coroutines.*
import me.vripper.event.EventBus
import me.vripper.event.SettingsUpdateEvent
import me.vripper.model.Settings
import me.vripper.services.AppEndpointService
import me.vripper.services.SettingsService
import tornadofx.runLater

class ClipboardService(
    private val appEndpointService: AppEndpointService,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) {
    private var current: String? = null
    private var coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null

    fun init() {
        eventBus.events.ofType(SettingsUpdateEvent::class.java).subscribe {
            run(it.settings)
        }
        run(settingsService.settings)
    }

    private fun run(settings: Settings) {
        pollJob?.cancel()
        if (settings.systemSettings.enableClipboardMonitoring) {
            pollJob = coroutineScope.launch {
                while (isActive) {
                    runLater {
                        poll()
                    }
                    delay(settings.systemSettings.clipboardPollingRate.toLong())
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
}