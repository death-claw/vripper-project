package me.vripper.repositories

import me.vripper.entities.Thread
import java.util.*

interface ThreadRepository {
    fun save(thread: Thread): Thread
    fun findByThreadId(threadId: Long): Optional<Thread>
    fun findAll(): List<Thread>
    fun findById(id: Long): Optional<Thread>
    fun deleteByThreadId(threadId: Long): Int
    fun deleteAll()
}