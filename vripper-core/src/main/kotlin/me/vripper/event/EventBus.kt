package me.vripper.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {

    private val _events = MutableSharedFlow<Any>(0, Int.MAX_VALUE, BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    fun publishEvent(event: Any) {
        _events.tryEmit(event)
    }
}
