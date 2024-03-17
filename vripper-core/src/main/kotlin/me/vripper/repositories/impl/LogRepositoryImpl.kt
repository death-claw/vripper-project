package me.vripper.repositories.impl

import me.vripper.entities.LogEntryEntity
import me.vripper.repositories.LogRepository
import me.vripper.services.SettingsService
import me.vripper.tables.LogTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import java.util.*

class LogRepositoryImpl(val settingsService: SettingsService) : LogRepository {

    @Synchronized
    override fun save(logEntryEntity: LogEntryEntity): LogEntryEntity {
        val id = LogTable.insertAndGetId {
            it[type] = logEntryEntity.type.name
            it[status] = logEntryEntity.status.name
            it[time] = logEntryEntity.time
            it[message] = logEntryEntity.message.take(500)
        }.value
        return logEntryEntity.copy(id = id)
    }

    override fun update(logEntryEntity: LogEntryEntity) {
        LogTable.update({ LogTable.id eq logEntryEntity.id }) {
            it[type] = logEntryEntity.type.name
            it[status] = logEntryEntity.status.name
            it[message] = logEntryEntity.message.take(500)
        }
    }

    override fun findById(id: Long): Optional<LogEntryEntity> {
        val result = LogTable.select {
            LogTable.id eq id
        }.map(::transform)

        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findAll(): List<LogEntryEntity> {
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

    private fun transform(resultRow: ResultRow): LogEntryEntity {
        val id = resultRow[LogTable.id].value
        val type = LogEntryEntity.Type.valueOf(resultRow[LogTable.type])
        val status = LogEntryEntity.Status.valueOf(resultRow[LogTable.status])
        val time = resultRow[LogTable.time]
        val message = resultRow[LogTable.message]
        return LogEntryEntity(
            id,
            type,
            status,
            time,
            message
        )
    }
}
