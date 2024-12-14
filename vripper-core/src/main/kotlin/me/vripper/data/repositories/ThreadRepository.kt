package me.vripper.data.repositories

import me.vripper.entities.ThreadEntity
import java.util.*

internal interface ThreadRepository {
    fun save(threadEntity: ThreadEntity): ThreadEntity
    fun update(threadEntity: ThreadEntity)
    fun findByThreadId(threadId: Long): Optional<ThreadEntity>
    fun findAll(): List<ThreadEntity>
    fun findById(id: Long): Optional<ThreadEntity>
    fun deleteByThreadId(threadId: Long): Int
    fun deleteAll()
}