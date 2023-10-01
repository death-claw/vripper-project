package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.repositories.ThreadRepository
import me.mnlr.vripper.tables.ThreadTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ThreadRepositoryImpl(private val eventBus: EventBus) : ThreadRepository {

    override fun save(thread: Thread): Thread {
        val insertStatement = ThreadTable.insert {
            it[title] = thread.title
            it[total] = thread.total
            it[url] = thread.link
            it[threadId] = thread.threadId
        }
        eventBus.publishEvent(
            Event(
                Event.Kind.THREAD_UPDATE, insertStatement[ThreadTable.id].value
            )
        )
        return thread.copy(id = insertStatement[ThreadTable.id].value)
    }

    override fun findByThreadId(threadId: String): Optional<Thread> {
        val result = ThreadTable.select {
            ThreadTable.threadId eq threadId
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findAll(): List<Thread> {
        return ThreadTable.selectAll().map(this::transform)
    }

    override fun findById(id: Long): Optional<Thread> {
        val result = ThreadTable.select {
            ThreadTable.id eq id
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun deleteByThreadId(threadId: String): Int {
        val mutationCount = ThreadTable.deleteWhere { ThreadTable.threadId eq threadId }
        eventBus.publishEvent(Event(Event.Kind.THREAD_REMOVE, threadId))
        return mutationCount
    }

    override fun deleteAll() {
        ThreadTable.deleteAll()
        eventBus.publishEvent(Event(Event.Kind.THREAD_CLEAR, null))
    }

    private fun transform(resultRow: ResultRow): Thread {
        val id = resultRow[ThreadTable.id].value
        val title = resultRow[ThreadTable.title]
        val url = resultRow[ThreadTable.url]
        val threadId = resultRow[ThreadTable.threadId]
        val total = resultRow[ThreadTable.total]
        return Thread(id, title, url, threadId, total)
    }
}
