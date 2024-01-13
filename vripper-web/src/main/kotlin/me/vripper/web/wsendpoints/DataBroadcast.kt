package me.vripper.web.wsendpoints

import jakarta.annotation.PostConstruct
import me.vripper.event.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class DataBroadcast(
    private val template: SimpMessagingTemplate
) : KoinComponent {

    private val eventBus: EventBus by inject()

    @PostConstruct
    private fun run() {
        eventBus.events.ofType(PostCreateEvent::class.java).subscribe { events ->
            template.convertAndSend("/topic/posts/new", events.posts)
        }
        eventBus.events.ofType(PostUpdateEvent::class.java).subscribe { events ->
            template.convertAndSend("/topic/posts/updated", events.posts)
        }
        eventBus.events.ofType(PostDeleteEvent::class.java).subscribe { events ->
            template.convertAndSend("/topic/posts/deleted", events.postIds)
        }

        eventBus.events.ofType(QueueStateEvent::class.java).subscribe {
            template.convertAndSend("/topic/queue-state", it.queueState)
        }

        eventBus.events.ofType(DownloadSpeedEvent::class.java).subscribe {
            template.convertAndSend("/topic/download-speed", it.downloadSpeed)
        }

        eventBus.events.ofType(VGUserLoginEvent::class.java).subscribe {
            template.convertAndSend("/topic/vg-username", it.username)
        }

        eventBus.events.ofType(ErrorCountEvent::class.java).subscribe {
            template.convertAndSend("/topic/error-count", it.errorCount)
        }

        eventBus.events.ofType(LoadingTasks::class.java).sample(Duration.ofMillis(500)).subscribe {
            template.convertAndSend("/topic/loading", it.loading)
        }

        eventBus.events.ofType(ImageEvent::class.java).buffer(Duration.ofMillis(500)).subscribe { events ->
            events.reversed().map { it.images }.flatten().distinct().groupBy { it.postId }
                .forEach {
                    template.convertAndSend("/topic/images/${it.key}", it.value)
                }
        }

        eventBus.events.ofType(ThreadCreateEvent::class.java).map { listOf(it.thread) }
            .subscribe { events ->
                template.convertAndSend("/topic/threads", events)
            }

        eventBus.events.ofType(ThreadDeleteEvent::class.java).map { listOf(it.threadId) }
            .subscribe {
                template.convertAndSend("/topic/threads/deleted", it)
            }

        eventBus.events.ofType(ThreadClearEvent::class.java).map { listOf(true) }.subscribe {
            template.convertAndSend("/topic/threads/deletedAll", it)
        }

        eventBus.events.ofType(LogCreateEvent::class.java).map { it.logEntry }
            .subscribe { logCreateEvent ->
                template.convertAndSend("/topic/logs/new", listOf(logCreateEvent))
            }

        eventBus.events.ofType(LogUpdateEvent::class.java).map { it.logEntry }
            .subscribe { logUpdateEvent ->
                template.convertAndSend("/topic/logs/updated", listOf(logUpdateEvent))
            }

        eventBus.events.ofType(LogDeleteEvent::class.java).subscribe {
            template.convertAndSend("/topic/logs/deleted", listOf(it.deleted))
        }
    }
}