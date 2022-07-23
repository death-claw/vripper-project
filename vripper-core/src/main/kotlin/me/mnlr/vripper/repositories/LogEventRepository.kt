package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.LogEvent
import java.util.*

interface LogEventRepository {
    fun save(logEvent: LogEvent): LogEvent
    fun update(logEvent: LogEvent): LogEvent
    fun findAll(): List<LogEvent>
    fun findById(id: Long): Optional<LogEvent>
    fun delete(id: Long)
    fun deleteAll()
}