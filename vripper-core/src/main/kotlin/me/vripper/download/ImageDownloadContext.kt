package me.vripper.download

import kotlinx.coroutines.*
import me.vripper.entities.ImageEntity
import me.vripper.model.Settings
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.koin.core.component.KoinComponent

internal class ImageDownloadContext(val imageEntity: ImageEntity, val settings: Settings) : KoinComponent {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableListOf<Job>()
    val httpContext: HttpClientContext =
        HttpClientContext.create().apply { cookieStore = BasicCookieStore() }
    val requests = mutableListOf<HttpUriRequestBase>()
    val postId = imageEntity.postIdRef

    fun cancelCoroutines() {
        runBlocking {
            coroutineScope.cancel()
            jobs.forEach { job -> job.cancelAndJoin() }
        }
    }

    fun launchCoroutine(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch(block = block).also { job -> jobs.add(job) }
    }
}