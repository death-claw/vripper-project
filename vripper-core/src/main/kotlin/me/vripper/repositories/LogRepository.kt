package me.vripper.repositories

import me.vripper.entities.LogEntryEntity
import java.util.*

interface LogRepository {
    fun save(logEntryEntity: LogEntryEntity): LogEntryEntity
    fun update(logEntryEntity: LogEntryEntity)
    fun findAll(): List<LogEntryEntity>
    fun findById(id: Long): Optional<LogEntryEntity>

    fun deleteOldest(): List<Long>
    fun deleteAll()
}