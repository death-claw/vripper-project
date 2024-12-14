package me.vripper.services

import kotlinx.coroutines.*
import me.vripper.RingAppender
import me.vripper.entities.LogEntryEntity
import me.vripper.event.EventBus
import me.vripper.event.LogCreateEvent
import me.vripper.utilities.StackTraceElementUtils
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LogService(private val eventBus: EventBus) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private var job: Job? = null

    fun init() {
        if (job != null) {
            return
        }
        job = coroutineScope.launch {
            while (isActive) {
                while (!RingAppender.Static.events.isEmpty()) {
                    RingAppender.Static.events.poll()?.also { loggingEvent ->
                        eventBus.publishEvent(
                            LogCreateEvent(
                                LogEntryEntity(
                                    loggingEvent.sequenceNumber,
                                    loggingEvent.instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
                                        .format(dateTimeFormatter),
                                    loggingEvent.threadName,
                                    loggingEvent.loggerName,
                                    loggingEvent.level.levelStr,
                                    loggingEvent.formattedMessage,
                                    loggingEvent.throwableProxy?.let { throwableProxy ->
                                        StackTraceElementUtils.format(
                                            throwableProxy
                                        )
                                    } ?: ""
                                )
                            )
                        )
                    }
                }
                delay(777)
            }
        }
    }
}