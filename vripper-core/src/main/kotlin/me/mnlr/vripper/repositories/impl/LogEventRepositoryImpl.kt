package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.repositories.LogEventRepository
import java.util.*

class LogEventRepositoryImpl : LogEventRepository {

    @Synchronized
    override fun save(logEvent: LogEvent): LogEvent {
        return  logEvent
    }

    override fun update(logEvent: LogEvent): LogEvent {
        return logEvent
    }

    override fun findById(id: Long): Optional<LogEvent> {
        return Optional.empty()
    }

    override fun findAll(): List<LogEvent> {
        return emptyList()
    }

    override fun delete(id: Long) {

    }

    override fun deleteAll() {

    }
}
