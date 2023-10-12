package me.mnlr.vripper.gui.view

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotlinx.coroutines.asCoroutineDispatcher
import me.mnlr.vripper.ApplicationProperties.BASE_DIR_NAME
import me.mnlr.vripper.ApplicationProperties.baseDir
import me.mnlr.vripper.hash256
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.io.path.Path

object PreviewCache {

    private const val CACHE_DIR_NAME = "cache"
    val previewDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val cachePath = Path(baseDir, BASE_DIR_NAME, CACHE_DIR_NAME)

    init {
        Files.createDirectories(cachePath)
    }

    val cache: LoadingCache<String, ByteArray> = Caffeine
        .newBuilder()
        .weigher { _: String, value: ByteArray -> value.size }
        .maximumWeight(1024 * 1024 * 100)
        .build(::load)

    fun load(url: String): ByteArray {
        val path = cachePath.resolve(url.hash256())
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return Files.readAllBytes(path)
        }
        return URL(url).openStream().use { `is` ->
            val bytes = `is`.readAllBytes()
            Files.write(path, bytes)
            bytes
        }
    }
}
