package me.mnlr.vripper.repositories.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.repositories.ThreadRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@Service
class ThreadRepositoryImpl @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate,
    private val eventBus: EventBus
) : ThreadRepository {
    @Synchronized
    private fun nextId(): Long {
        return jdbcTemplate.queryForObject("CALL NEXT VALUE FOR SEQ_THREAD", Long::class.java)!!
    }

    override fun save(thread: Thread): Thread {
        val id = nextId()
        jdbcTemplate.update(
            "INSERT INTO THREAD (ID, TOTAL, LINK, THREAD_ID) values (?,?,?,?)",
            id,
            thread.total,
            thread.link,
            thread.threadId
        )
        eventBus.publishEvent(Event(Event.Kind.THREAD_UPDATE, id))
        return thread.copy(id = id)
    }

    override fun findByThreadId(threadId: String): Optional<Thread> {
        val threadList = jdbcTemplate.query(
            "SELECT * FROM THREAD AS thread WHERE thread.THREAD_ID = ?",
            ThreadRowMapper(),
            threadId
        )
        return if (threadList.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(threadList[0])
        }
    }

    override fun findAll(): List<Thread> {
        return jdbcTemplate.query("SELECT * FROM THREAD", ThreadRowMapper())
    }

    override fun findById(id: Long): Optional<Thread> {
        val threadList = jdbcTemplate.query(
            "SELECT * FROM THREAD AS thread WHERE thread.ID = ?", ThreadRowMapper(), id
        )
        return if (threadList.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(threadList[0])
        }
    }

    override fun deleteByThreadId(threadId: String): Int {
        val mutationCount =
            jdbcTemplate.update("DELETE FROM THREAD AS thread WHERE THREAD_ID = ?", threadId)
        eventBus.publishEvent(Event(Event.Kind.THREAD_REMOVE, threadId))
        return mutationCount
    }

    override fun deleteAll() {
        jdbcTemplate.update("DELETE FROM THREAD")
        eventBus.publishEvent(Event(Event.Kind.THREAD_CLEAR, null))
    }
}

internal class ThreadRowMapper : RowMapper<Thread> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): Thread {
        val id = rs.getLong("ID")
        val link = rs.getString("LINK")
        val threadId = rs.getString("THREAD_ID")
        val total = rs.getInt("TOTAL")
        return Thread(id, link, threadId, total)
    }
}