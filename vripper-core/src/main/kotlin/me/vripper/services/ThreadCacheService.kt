package me.vripper.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import me.vripper.event.EventBus
import me.vripper.event.SettingsUpdateEvent
import me.vripper.model.ThreadItem
import me.vripper.parser.ThreadLookupAPIParser
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ThreadCacheService(val eventBus: EventBus) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init() {
        coroutineScope.launch {
            eventBus.events.filterIsInstance(SettingsUpdateEvent::class).collect {
                cache.invalidateAll()
            }
        }
    }

    private val cache: LoadingCache<Long, ThreadItem> =
        Caffeine.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build { threadId ->
            ThreadLookupAPIParser(threadId).parse()
        }

    @Throws(ExecutionException::class)
    operator fun get(threadId: Long): ThreadItem {
        return cache[threadId] ?: throw NoSuchElementException("$threadId does not exist")
    }
}