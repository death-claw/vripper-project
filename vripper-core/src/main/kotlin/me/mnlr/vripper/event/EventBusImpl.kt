package me.mnlr.vripper.event

import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitFailureHandler
import reactor.core.publisher.Sinks.EmitResult

class EventBusImpl: EventBus {
    private val sink = Sinks.many().multicast().onBackpressureBuffer<Event<*>>()
    override fun publishEvent(event: Event<*>) {
        sink.emitNext(event, RETRY)
    }

    override fun flux(): Flux<Event<*>> {
        return sink.asFlux().publishOn(EventBusScheduler.scheduler)
    }

//    @PreDestroy
    private fun destroy() {
        sink.emitComplete(RETRY)
    }

    companion object {
        val RETRY = EmitFailureHandler { _: SignalType?, _: EmitResult? -> true }
    }
}