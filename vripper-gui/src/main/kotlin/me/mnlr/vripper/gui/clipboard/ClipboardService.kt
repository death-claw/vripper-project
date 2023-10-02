package me.mnlr.vripper.gui.clipboard

import javafx.scene.input.Clipboard
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.services.SettingsService
import reactor.core.Disposable

class ClipboardService(
    private val appEndpointService: AppEndpointService,
    private val settingsService: SettingsService,
    eventBus: EventBus
) {
    private val eventBusDisposable: Disposable
    private var current: String? = null
    private var coroutineScope = CoroutineScope(Dispatchers.JavaFx)

    init {
        eventBusDisposable = eventBus
            .flux()
            .filter { it.kind == Event.Kind.SETTINGS_UPDATE }
            .doOnNext { init() }.subscribe()
    }

    fun init() {
        coroutineScope.cancel()
        if(settingsService.settings.clipboardSettings.enable) {
            coroutineScope.launch {
                while (isActive) {
                    poll()
                    delay(settingsService.settings.clipboardSettings.pollingRate.toLong())
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