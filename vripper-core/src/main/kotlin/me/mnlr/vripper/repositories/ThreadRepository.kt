package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.Thread
import java.util.*

interface ThreadRepository {
    fun save(thread: Thread): Thread
    fun findByThreadId(threadId: String): Optional<Thread>
    fun findAll(): List<Thread>
    fun findById(id: Long): Optional<Thread>
    fun deleteByThreadId(threadId: String): Int
    fun deleteAll()
}