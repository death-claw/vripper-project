package me.vripper.utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.event.EventBus
import me.vripper.event.LoadingTasks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object Tasks : KoinComponent {

    private val eventBus: EventBus by inject()
    private var current = 0
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Synchronized
    fun increment() {
        if (current == 0) {
            coroutineScope.launch {
                eventBus.publishEvent(LoadingTasks(true))
            }
        }
        current += 1
    }

    @Synchronized
    fun decrement() {
        current -= 1
        if (current == 0) {
            coroutineScope.launch {
                eventBus.publishEvent(LoadingTasks(false))
            }
        }
    }
}
