package me.mnlr.vripper.clipboard

import jakarta.annotation.PostConstruct
import javafx.scene.input.Clipboard
import me.mnlr.vripper.AppEndpointService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import tornadofx.*

@Service
class ClipboardService(private val appEndpointService: AppEndpointService) {

    private val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
    private var current: String? = null

    @PostConstruct
    fun init() {
        sink.asFlux().subscribeOn(Schedulers.single()).subscribe {
            this.appEndpointService.scanLinks(it)
        }
        runLater {
            this.current = if(Clipboard.getSystemClipboard().hasString()) Clipboard.getSystemClipboard().string else null
        }
    }
    @Scheduled(fixedDelay = 500)
    fun poll() {
        runLater {
            val clipboard = Clipboard.getSystemClipboard()
            if(clipboard.hasString()) {
                val value: String? = clipboard.string
                if(!value.isNullOrBlank() && value != this.current) {
                    this.current = value
                    sink.emitNext(value) { _,_ ->
                        true
                    }
                }
            }
        }
    }
}