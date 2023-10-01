package me.mnlr.vripper.event

import reactor.core.publisher.Flux

interface EventBus {
    fun publishEvent(event: Event<*>)
    fun flux(): Flux<Event<*>>
}
