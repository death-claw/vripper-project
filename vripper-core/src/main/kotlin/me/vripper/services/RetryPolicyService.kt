package me.vripper.services

import me.vripper.event.EventBus
import me.vripper.event.SettingsUpdateEvent
import net.jodah.failsafe.RetryPolicy
import net.jodah.failsafe.event.ExecutionAttemptedEvent
import java.time.temporal.ChronoUnit

class RetryPolicyService(
    val eventBus: EventBus, settingsService: SettingsService
) {
    private val log by me.vripper.delegate.LoggerDelegate()
    private var maxAttempts: Int = settingsService.settings.connectionSettings.maxAttempts

    fun init() {
        eventBus.events.ofType(SettingsUpdateEvent::class.java).subscribe {
            if (maxAttempts != it.settings.connectionSettings.maxAttempts) {
                maxAttempts = it.settings.connectionSettings.maxAttempts
            }
        }

    }

    fun <T> buildRetryPolicyForDownload(): RetryPolicy<T> {
        return RetryPolicy<T>().withDelay(2, 5, ChronoUnit.SECONDS).withMaxAttempts(maxAttempts).onFailedAttempt {
                log.warn("#${it.attemptCount} tries failed", it.lastFailure)
            }
    }

    fun <T> buildGenericRetryPolicy(): RetryPolicy<T> {
        return RetryPolicy<T>().withDelay(2, 5, ChronoUnit.SECONDS).withMaxAttempts(maxAttempts)
            .onFailedAttempt { e: ExecutionAttemptedEvent<T> ->
                log.warn("#${e.attemptCount} tries failed", e.lastFailure)
            }
    }
}