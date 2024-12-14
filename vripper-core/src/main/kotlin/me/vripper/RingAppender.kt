package me.vripper

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.apache.commons.collections4.queue.CircularFifoQueue

class RingAppender : AppenderBase<ILoggingEvent>() {

    object Static {
        val events = CircularFifoQueue<ILoggingEvent>()
    }

    override fun append(event: ILoggingEvent) {
        Static.events.offer(event)
    }
}