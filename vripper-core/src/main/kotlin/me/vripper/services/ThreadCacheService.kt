package me.vripper.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import me.vripper.event.EventBus
import me.vripper.event.SettingsUpdateEvent
import me.vripper.vgapi.ThreadItem
import me.vripper.vgapi.ThreadLookupAPIParser
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

internal class ThreadCacheService(val eventBus: EventBus) {

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
            runBlocking {
                ThreadLookupAPIParser(threadId).parse()
            }
        }

    @Throws(ExecutionException::class)
    operator fun get(threadId: Long): ThreadItem {
        return cache[threadId]
    }

    fun getIfPresent(threadId: Long): ThreadItem? {
        return cache.getIfPresent(threadId)
    }
}