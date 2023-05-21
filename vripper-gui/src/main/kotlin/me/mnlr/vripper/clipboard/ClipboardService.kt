package me.mnlr.vripper.clipboard

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import javafx.scene.input.Clipboard
import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.services.SettingsService
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import tornadofx.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class ClipboardService(
    private val appEndpointService: AppEndpointService,
    private val settingsService: SettingsService,
    eventBus: EventBus
) {
    private val eventBusDisposable: Disposable
    private val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var current: String? = null
    private var scheduledFuture: ScheduledFuture<*>? = null

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

    @PostConstruct
    fun init() {
        scheduledFuture?.cancel(true)
        if(settingsService.settings.clipboardSettings.enable) {
            scheduledFuture = scheduler.scheduleWithFixedDelay({
                poll()
            }, 500, settingsService.settings.clipboardSettings.pollingRate.toLong(), TimeUnit.MILLISECONDS)
        } else {
            current = null
        }
    }

    fun poll() {
        runLater {
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
    }

    @PreDestroy
    fun destroy() {
        scheduledFuture?.cancel(true)
        scheduler.shutdown()
    }
}