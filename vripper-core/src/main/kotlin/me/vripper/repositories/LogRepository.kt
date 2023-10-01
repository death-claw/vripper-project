package me.vripper.repositories

import me.vripper.entities.LogEntry
import java.util.*

interface LogRepository {
    fun save(logEntry: LogEntry): LogEntry
    fun update(logEntry: LogEntry)
    fun findAll(): List<LogEntry>
    fun findById(id: Long): Optional<LogEntry>

    fun deleteOldest(): List<Long>
    fun deleteAll()
}