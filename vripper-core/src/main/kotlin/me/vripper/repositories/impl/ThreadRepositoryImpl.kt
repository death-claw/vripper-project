package me.vripper.repositories.impl

import me.vripper.entities.ThreadEntity
import me.vripper.repositories.ThreadRepository
import me.vripper.tables.ThreadTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ThreadRepositoryImpl : ThreadRepository {

    override fun save(threadEntity: ThreadEntity): ThreadEntity {
        val id = ThreadTable.insertAndGetId {
            it[title] = threadEntity.title
            it[total] = threadEntity.total
            it[url] = threadEntity.link
            it[threadId] = threadEntity.threadId
        }.value
        return threadEntity.copy(id = id)
    }

    override fun update(threadEntity: ThreadEntity) {
        ThreadTable.update({ ThreadTable.id eq threadEntity.id }) {
            it[title] = threadEntity.title
            it[total] = threadEntity.total
        }
    }

    override fun findByThreadId(threadId: Long): Optional<ThreadEntity> {
        val result = ThreadTable.select {
            ThreadTable.threadId eq threadId
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findAll(): List<ThreadEntity> {
        return ThreadTable.selectAll().map(this::transform)
    }

    override fun findById(id: Long): Optional<ThreadEntity> {
        val result = ThreadTable.select {
            ThreadTable.id eq id
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun deleteByThreadId(threadId: Long): Int {
        return ThreadTable.deleteWhere { ThreadTable.threadId eq threadId }
    }

    override fun deleteAll() {
        ThreadTable.deleteAll()
    }

    private fun transform(resultRow: ResultRow): ThreadEntity {
        val id = resultRow[ThreadTable.id].value
        val title = resultRow[ThreadTable.title]
        val url = resultRow[ThreadTable.url]
        val threadId = resultRow[ThreadTable.threadId]
        val total = resultRow[ThreadTable.total]
        return ThreadEntity(id, title, url, threadId, total)
    }
}
