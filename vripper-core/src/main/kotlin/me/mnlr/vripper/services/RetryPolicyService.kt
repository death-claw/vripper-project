package me.mnlr.vripper.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.SettingsUpdateEvent
import net.jodah.failsafe.RetryPolicy
import net.jodah.failsafe.event.ExecutionAttemptedEvent
import java.time.temporal.ChronoUnit

class RetryPolicyService(
    val eventBus: EventBus,
    settingsService: SettingsService
) {
    private val log by LoggerDelegate()
    private var maxAttempts: Int = settingsService.settings.connectionSettings.maxAttempts
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init() {
        coroutineScope.launch {
            eventBus.subscribe<SettingsUpdateEvent> {
                if (maxAttempts != it.settings.connectionSettings.maxAttempts) {
                    maxAttempts = it.settings.connectionSettings.maxAttempts
                }
            }
        }
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
}