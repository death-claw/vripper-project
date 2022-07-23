package me.mnlr.vripper.web.wsendpoints

import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.formatSI
import me.mnlr.vripper.model.DownloadSpeed
import me.mnlr.vripper.model.GlobalState
import me.mnlr.vripper.model.LoggedUser
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.repositories.ThreadRepository
import me.mnlr.vripper.services.DownloadSpeedService
import me.mnlr.vripper.services.GlobalStateService
import me.mnlr.vripper.services.VGAuthService

@Controller
class DataController constructor(
    vgAuthService: VGAuthService,
    globalStateService: GlobalStateService,
    downloadSpeedService: DownloadSpeedService,
    appEndpointService: AppEndpointService,
    postDownloadStateRepository: PostDownloadStateRepository,
    imageRepository: ImageRepository,
    threadRepository: ThreadRepository,
    logEventRepository: LogEventRepository
) {
    private val appEndpointService: AppEndpointService
    private val vgAuthService: VGAuthService
    private val globalStateService: GlobalStateService
    private val downloadSpeedService: DownloadSpeedService
    private val postDownloadStateRepository: PostDownloadStateRepository
    private val imageRepository: ImageRepository
    private val threadRepository: ThreadRepository
    private val logEventRepository: LogEventRepository

    init {
        this.vgAuthService = vgAuthService
        this.globalStateService = globalStateService
        this.downloadSpeedService = downloadSpeedService
        this.appEndpointService = appEndpointService
        this.postDownloadStateRepository = postDownloadStateRepository
        this.imageRepository = imageRepository
        this.threadRepository = threadRepository
        this.logEventRepository = logEventRepository
    }

    @SubscribeMapping("/state")
    fun downloadState(): GlobalState {
        return globalStateService.get()
    }

    @SubscribeMapping("/posts")
    fun posts(): Collection<PostDownloadState> {
        return postDownloadStateRepository.findAll()
    }

    @SubscribeMapping("/images/{postId}")
    fun postsDetails(@DestinationVariable("postId") postId: String): List<ImageDownloadState> {
        return imageRepository.findByPostId(postId)
    }

    @SubscribeMapping("/queued")
    fun queued(): Collection<Thread> {
        return threadRepository.findAll()
    }

    @SubscribeMapping("/events")
    fun events(): Collection<LogEvent> {
        return logEventRepository.findAll()
    }
}