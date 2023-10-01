package me.vripper.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import me.vripper.event.EventBus
import me.vripper.event.SettingsUpdateEvent
import me.vripper.model.ThreadItem
import me.vripper.parser.ThreadLookupAPIParser
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ThreadCacheService(val eventBus: EventBus) {

    fun init() {
        eventBus.events.ofType(SettingsUpdateEvent::class.java).subscribe {
            cache.invalidateAll()
        }
    }

    private val cache: LoadingCache<Long, ThreadItem> =
        Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build { threadId ->
            ThreadLookupAPIParser(threadId).parse()
        }

    @Throws(ExecutionException::class)
    operator fun get(threadId: Long): ThreadItem {
        return cache[threadId] ?: throw NoSuchElementException("$threadId does not exist")
    }
}