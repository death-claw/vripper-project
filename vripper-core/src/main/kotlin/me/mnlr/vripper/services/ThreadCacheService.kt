package me.mnlr.vripper.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.SettingsUpdateEvent
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.parser.ThreadLookupAPIParser
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ThreadCacheService(val eventBus: EventBus) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init() {
        coroutineScope.launch {
            eventBus.subscribe<SettingsUpdateEvent> {
                cache.invalidateAll()
            }
        }
    }

    private val cache: LoadingCache<String, ThreadItem> =
        Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build { threadId: String ->
                ThreadLookupAPIParser(threadId).parse()
            }

    @Throws(ExecutionException::class)
    operator fun get(threadId: String): ThreadItem {
        return cache[threadId] ?: throw NoSuchElementException("$threadId does not exist")
    }
}