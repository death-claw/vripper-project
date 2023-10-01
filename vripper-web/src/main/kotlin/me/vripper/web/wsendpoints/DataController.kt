package me.vripper.web.wsendpoints

import me.vripper.entities.Image
import me.vripper.entities.LogEntry
import me.vripper.entities.Post
import me.vripper.entities.Thread
import me.vripper.model.DownloadSpeed
import me.vripper.model.ErrorCount
import me.vripper.model.QueueState
import me.vripper.services.DataTransaction
import me.vripper.services.VGAuthService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller

@Controller
class DataController : KoinComponent {
    private val dataTransaction: DataTransaction by inject()
    private val vgAuthService: VGAuthService by inject()

    @SubscribeMapping("/queue-state")
    fun queueState(): QueueState {
        return QueueState(0, 0)
    }

    @SubscribeMapping("/download-speed")
    fun downloadSpeed(): DownloadSpeed {
        return DownloadSpeed(0L)
    }

    @SubscribeMapping("/vg-username")
    fun vgUsername(): String {
        return vgAuthService.loggedUser
    }

    @SubscribeMapping("/error-count")
    fun errorCount(): ErrorCount {
        return ErrorCount(0)
    }

    @SubscribeMapping("/posts/new")
    fun posts(): Collection<Post> {
        return dataTransaction.findAllPosts()
    }

    @SubscribeMapping("/images/{postId}")
    fun postsDetails(@DestinationVariable("postId") postId: Long): List<Image> {
        return dataTransaction.findImagesByPostId(postId)
    }

    @SubscribeMapping("/threads")
    fun queued(): List<Thread> {
        return dataTransaction.findAllThreads()
    }

    @SubscribeMapping("/logs/new")
    fun events(): List<LogEntry> {
        return dataTransaction.findAllLogs()
    }
}