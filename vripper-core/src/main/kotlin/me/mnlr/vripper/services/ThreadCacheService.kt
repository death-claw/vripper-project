package me.mnlr.vripper.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.parser.ThreadLookupAPIParser
import reactor.core.Disposable
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ThreadCacheService(eventBus: EventBus) {

    private val cache: LoadingCache<String, ThreadItem> =
        Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
            .build { threadId: String ->
                ThreadLookupAPIParser(threadId).parse()
            }

    private val disposable: Disposable =
        eventBus.flux().filter { e: Event<*> -> e.kind == Event.Kind.SETTINGS_UPDATE }
            .doOnNext { cache.invalidateAll() }.subscribe()

    private fun destroy() {
        disposable.dispose()
    }

    @Throws(ExecutionException::class)
    operator fun get(threadId: String): ThreadItem {
        return cache[threadId] ?: throw NoSuchElementException("$threadId does not exist")
    }
}