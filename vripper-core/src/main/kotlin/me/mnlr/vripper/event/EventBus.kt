package me.mnlr.vripper.event

import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitFailureHandler
import reactor.core.publisher.Sinks.EmitResult

@Service
class EventBus {
    private val sink = Sinks.many().multicast().onBackpressureBuffer<Event<*>>()
    fun publishEvent(event: Event<*>) {
        sink.emitNext(event, RETRY)
    }

    fun flux(): Flux<Event<*>> {
        return sink.asFlux()
    }

    @PreDestroy
    private fun destroy() {
        sink.emitComplete(RETRY)
    }

    companion object {
        val RETRY = EmitFailureHandler { _: SignalType?, _: EmitResult? -> true }
    }
}