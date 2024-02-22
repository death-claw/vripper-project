package me.vripper.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {

    private val _events = MutableSharedFlow<Any>()
    val events = _events.asSharedFlow()

    suspend fun publishEvent(event: Any) {
        _events.emit(event)
    }
}
