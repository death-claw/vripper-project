package me.mnlr.vripper.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import java.util.concurrent.atomic.AtomicLong

@Service
class DownloadSpeedService(private val eventBus: EventBus) {
    private val read = AtomicLong(0)
    var currentValue = 0L
    private var allowWrite = false
    fun increase(read: Long) {
        if (allowWrite) {
            this.read.addAndGet(read)
        }
    }

    @Scheduled(fixedDelay = 1000)
    private fun calc() {
        allowWrite = false
        val newValue = read.getAndSet(0)
        if (newValue != currentValue) {
            currentValue = newValue
            eventBus.publishEvent(Event(Event.Kind.BYTES_PER_SECOND, currentValue))
        }
        allowWrite = true
    }
}