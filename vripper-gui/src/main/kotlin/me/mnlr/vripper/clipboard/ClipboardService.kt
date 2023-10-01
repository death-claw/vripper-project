package me.mnlr.vripper.clipboard

import javafx.scene.input.Clipboard
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.services.SettingsService
import reactor.core.Disposable
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import tornadofx.*

class ClipboardService(
    private val appEndpointService: AppEndpointService,
    private val settingsService: SettingsService,
    eventBus: EventBus
) {
    private val eventBusDisposable: Disposable
    private val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
    private var current: String? = null
    private var coroutineScope = CoroutineScope(Dispatchers.JavaFx)

    init {
        runLater {
            this.current = if (Clipboard.getSystemClipboard()
                    .hasString()
            ) Clipboard.getSystemClipboard().string else null
        }
        eventBusDisposable = eventBus
            .flux()
            .filter { it.kind == Event.Kind.SETTINGS_UPDATE }
            .subscribe { init() }
        sink.asFlux().subscribeOn(Schedulers.single()).subscribe {
            this.appEndpointService.scanLinks(it)
        }
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
                sink.emitNext(value) { _, _ ->
                    true
                }
            }
        }
    }

    fun destroy() {
        coroutineScope.cancel()
    }
}