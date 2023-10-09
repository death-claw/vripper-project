package me.mnlr.vripper.gui.view

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import java.net.URL

object PreviewCache {
    val cache: LoadingCache<String, ByteArray> = Caffeine
        .newBuilder()
        .weigher { _: String, value: ByteArray -> value.size }
        .maximumWeight(1024 * 1024 * 100)
        .build(::loadByteArray)

    fun loadByteArray(url: String): ByteArray {
        return URL(url).openStream().use { `is` -> `is`.readAllBytes() }
    }
}