package me.vripper.tasks

import me.vripper.event.EventBus
import me.vripper.event.LoadingTasks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal object Tasks : KoinComponent {

    private val eventBus: EventBus by inject()
    private var current = 0

    @Synchronized
    fun increment() {
        if (current == 0) {
            eventBus.publishEvent(LoadingTasks(true))
        }
        current += 1
    }

    @Synchronized
    fun decrement() {
        current -= 1
        if (current == 0) {
            eventBus.publishEvent(LoadingTasks(false))
        }
    }
}
