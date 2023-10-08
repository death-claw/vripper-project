package me.mnlr.vripper.gui.view

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import java.net.URL

object PreviewCache {
    val cache: LoadingCache<String, ByteArray> = Caffeine
        .newBuilder()
        .weigher { _: String, value: ByteArray -> value.size }
        .maximumWeight(1024 * 1024 * 100)
        .build { URL(it).openStream().use { `is` -> `is`.readAllBytes() } }
}