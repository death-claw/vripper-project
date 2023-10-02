package me.mnlr.vripper.services

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.model.Settings
import net.jodah.failsafe.RetryPolicy
import net.jodah.failsafe.event.ExecutionAttemptedEvent
import reactor.core.Disposable
import java.time.temporal.ChronoUnit

class RetryPolicyService(
    eventBus: EventBus,
    settingsService: SettingsService
) {
    private val log by LoggerDelegate()

    private val disposable: Disposable
    private var maxAttempts: Int = settingsService.settings.connectionSettings.maxAttempts

    init {
        disposable = eventBus
            .flux()
            .filter { it.kind == Event.Kind.SETTINGS_UPDATE }
            .map { it.data as Settings }
            .doOnNext {
                if (maxAttempts != it.connectionSettings.maxAttempts) {
                    maxAttempts = it.connectionSettings.maxAttempts
                }
            }
            .subscribe()
    }

    fun <T> buildRetryPolicyForDownload(): RetryPolicy<T> {
        return RetryPolicy<T>()
            .withDelay(2, 5, ChronoUnit.SECONDS)
            .withMaxAttempts(maxAttempts)
            .onFailedAttempt {
                log.warn("#${it.attemptCount} tries failed", it.lastFailure)
            }
    }

    fun <T> buildGenericRetryPolicy(): RetryPolicy<T> {
        return RetryPolicy<T>()
            .withDelay(2, 5, ChronoUnit.SECONDS)
            .withMaxAttempts(maxAttempts)
            .onFailedAttempt { e: ExecutionAttemptedEvent<T> ->
                log.warn("#${e.attemptCount} tries failed", e.lastFailure)
            }
    }

    private fun destroy() {
        disposable.dispose()
    }
}