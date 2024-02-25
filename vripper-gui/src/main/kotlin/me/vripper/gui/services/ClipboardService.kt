package me.vripper.gui.services

import javafx.scene.input.Clipboard
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
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
        coroutineScope.launch {
            eventBus.events.filterIsInstance(SettingsUpdateEvent::class).collect {
                run(it.settings)
            }
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