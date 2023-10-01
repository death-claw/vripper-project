package me.vripper.repositories.impl

import me.vripper.entities.LogEntry
import me.vripper.repositories.LogRepository
import me.vripper.services.SettingsService
import me.vripper.tables.LogTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import java.util.*

class LogRepositoryImpl(val settingsService: SettingsService) : LogRepository {

    @Synchronized
    override fun save(logEntry: LogEntry): LogEntry {
        val id = LogTable.insertAndGetId {
            it[type] = logEntry.type.name
            it[status] = logEntry.status.name
            it[time] = logEntry.time
            it[message] = logEntry.message.take(500)
        }.value
        return logEntry.copy(id = id)
    }

    override fun update(logEntry: LogEntry) {
        LogTable.update({ LogTable.id eq logEntry.id }) {
            it[type] = logEntry.type.name
            it[status] = logEntry.status.name
            it[message] = logEntry.message.take(500)
        }
    }

    override fun findById(id: Long): Optional<LogEntry> {
        val result = LogTable.select {
            LogTable.id eq id
        }.map(::transform)

        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findAll(): List<LogEntry> {
        return LogTable.selectAll().map(::transform)
    }

    override fun deleteOldest(): List<Long> {
        val sub = LogTable.slice(LogTable.id).selectAll().orderBy(LogTable.time to SortOrder.DESC)
            .limit(settingsService.settings.systemSettings.maxEventLog)
        val ids = LogTable.select { LogTable.id notInSubQuery sub }.map { it[LogTable.id].value }
        LogTable.deleteWhere { LogTable.id notInSubQuery sub }
        return ids
    }

    override fun deleteAll() {
        LogTable.deleteAll()
    }

    private fun transform(resultRow: ResultRow): LogEntry {
        val id = resultRow[LogTable.id].value
        val type = LogEntry.Type.valueOf(resultRow[LogTable.type])
        val status = LogEntry.Status.valueOf(resultRow[LogTable.status])
        val time = resultRow[LogTable.time]
        val message = resultRow[LogTable.message]
        return LogEntry(
            id,
            type,
            status,
            time,
            message
        )
    }
}
