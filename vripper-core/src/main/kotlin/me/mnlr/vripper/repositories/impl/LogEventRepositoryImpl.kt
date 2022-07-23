package me.mnlr.vripper.repositories.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.services.SettingsService
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class LogEventRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val settingsService: SettingsService,
    private val eventBus: EventBus
) : LogEventRepository {

    private val log by LoggerDelegate()

    @Synchronized
    private fun nextId(): Long {
        return jdbcTemplate.queryForObject("CALL NEXT VALUE FOR SEQ_EVENT", Long::class.java)!!
    }

    @Synchronized
    override fun save(logEvent: LogEvent): LogEvent {
        val maxRecords = settingsService.settings.maxEventLog - 1
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM EVENT", Long::class.java) ?: 0
        if (count > maxRecords) {
            val idList = jdbcTemplate.queryForList(
                "SELECT ID FROM EVENT ORDER BY TIME ASC LIMIT ?",
                Long::class.java,
                count - maxRecords
            )
            idList.forEach { delete(it) }
        }
        val id = nextId()
        jdbcTemplate.update(
            "INSERT INTO EVENT (ID, TYPE, STATUS, TIME, MESSAGE) VALUES (?,?,?,?,?)",
            id,
            logEvent.type.name,
            logEvent.status.name,
            logEvent.time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            logEvent.message
        )
        eventBus.publishEvent(Event(Event.Kind.LOG_EVENT_UPDATE, id))
        return logEvent.copy(id = id)
    }

    override fun update(logEvent: LogEvent): LogEvent {
        if (logEvent.id == null) {
            log.warn("Cannot update entity with null id")
            return logEvent
        }
        jdbcTemplate.update(
            "UPDATE EVENT SET STATUS = ?, MESSAGE = ? WHERE ID = ?",
            logEvent.status.name,
            logEvent.message,
            logEvent.id
        )
        eventBus.publishEvent(Event(Event.Kind.LOG_EVENT_UPDATE, logEvent.id))
        return logEvent
    }

    override fun findById(id: Long): Optional<LogEvent> {
        val logEvents =
            jdbcTemplate.query("SELECT * FROM EVENT WHERE ID = ?", LogEventRowMapper(), id)
        return if (logEvents.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(logEvents[0])
        }
    }

    override fun findAll(): List<LogEvent> {
        return jdbcTemplate.query("SELECT * FROM EVENT", LogEventRowMapper())
    }

    override fun delete(id: Long) {
        jdbcTemplate.update("DELETE FROM EVENT WHERE ID = ?", id)
        eventBus.publishEvent(Event(Event.Kind.LOG_EVENT_REMOVE, id))
    }

    override fun deleteAll() {
        jdbcTemplate.update("DELETE FROM EVENT")
    }
}

internal class LogEventRowMapper : RowMapper<LogEvent> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): LogEvent {
        val id = rs.getLong("ID")
        val type = LogEvent.Type.valueOf(rs.getString("TYPE"))
        val status = LogEvent.Status.valueOf(rs.getString("STATUS"))
        val time =
            LocalDateTime.parse(rs.getString("TIME"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val message = rs.getString("MESSAGE")
        return LogEvent(id, type, status, time, message)
    }
}