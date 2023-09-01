package me.mnlr.vripper.web.wsendpoints

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import reactor.core.Disposable
import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.formatSI
import me.mnlr.vripper.model.GlobalState
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.repositories.LogEventRepository
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.repositories.ThreadRepository
import java.time.Duration
import java.time.temporal.ChronoUnit

@Service
class DataBroadcast constructor(
    private val template: SimpMessagingTemplate,
    private val eventBus: EventBus,
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val imageRepository: ImageRepository,
    private val threadRepository: ThreadRepository,
    private val logEventRepository: LogEventRepository
) {
    var globalState: GlobalState = GlobalState(0, 0, 0, "", "")
    private var disposable: Disposable? = null

    @PostConstruct
    private fun run() {
        disposable = eventBus
            .flux()
            .buffer(Duration.of(500, ChronoUnit.MILLIS))
            .subscribe { events: List<Event<*>> ->
                events.groupBy { it.kind }.forEach { (kind: Event.Kind, eventList: List<Event<*>>) ->
                    when (kind) {
                        Event.Kind.POST_UPDATE, Event.Kind.METADATA_UPDATE -> template.convertAndSend(
                            "/topic/posts",
                            eventList
                                .asSequence()
                                .map { (_, data): Event<*> -> data as Long }
                                .distinct()
                                .map { postDownloadStateRepository.findById(it) }
                                .filter { it.isPresent }
                                .map { it.get() }
                                .toList())

                        Event.Kind.POST_REMOVE -> template.convertAndSend(
                            "/topic/posts/deleted",
                            eventList.map { (_, data): Event<*> -> data as String })

                        Event.Kind.IMAGE_UPDATE -> eventList
                            .asSequence()
                            .map { (_, data): Event<*> -> data as Long }
                            .distinct()
                            .map { imageRepository.findById(it) }
                            .filter { it.isPresent }
                            .map { it.get() }
                            .groupBy { it.postId }
                            .forEach { (postId: String, images: List<ImageDownloadState>) ->
                                template.convertAndSend(
                                    "/topic/images/$postId", images
                                )
                            }

                        Event.Kind.THREAD_UPDATE -> template.convertAndSend(
                            "/topic/threads",
                            eventList
                                .asSequence()
                                .map { (_, data): Event<*> -> data as Long }
                                .distinct()
                                .map { threadRepository.findById(it) }
                                .filter { it.isPresent }
                                .map { it.get() }
                                .toList())

                        Event.Kind.THREAD_REMOVE -> template.convertAndSend(
                            "/topic/threads/deleted",
                            eventList
                                .map { (_, data): Event<*> -> data as String })

                        Event.Kind.THREAD_CLEAR -> template.convertAndSend(
                            "/topic/threads/deletedAll",
                            eventList
                                .map { true })

                        Event.Kind.LOG_EVENT_UPDATE -> template.convertAndSend(
                            "/topic/logs",
                            eventList
                                .asSequence()
                                .map { (_, data): Event<*> -> data as Long }
                                .distinct()
                                .map { logEventRepository.findById(it) }
                                .filter { it.isPresent }
                                .map { it.get() }
                                .toList())

                        Event.Kind.LOG_EVENT_REMOVE -> template.convertAndSend(
                            "/topic/logs/deleted",
                            eventList
                                .map { (_, data): Event<*> -> data as Long })

                        Event.Kind.VG_USER -> eventList
                            .distinct()
                            .forEach {
                                globalState = globalState.copy(loggedUser = it.data as String)
                                template.convertAndSend("/topic/state", globalState)
                            }
                        Event.Kind.DOWNLOAD_STATUS -> eventList
                            .distinct()
                            .forEach {
                                val state = it.data as GlobalState
                                globalState = globalState.copy(running = state.running, remaining = state.remaining, error = state.error)
                                template.convertAndSend("/topic/state", globalState)
                            }
                        Event.Kind.BYTES_PER_SECOND -> eventList
                            .distinct()
                            .forEach {
                                globalState = globalState.copy(downloadSpeed = (it.data as Long).formatSI())
                                template.convertAndSend("/topic/state", globalState)
                            }

                        else -> {}
                    }
                }
            }
    }

    @PreDestroy
    private fun destroy() {
        if (disposable != null) {
            disposable!!.dispose()
        }
    }
}