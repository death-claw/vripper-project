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
        eventBus.events.ofType(PostEvent::class.java).buffer(Duration.ofMillis(125)).subscribe { events ->

            events.map { it.delete }.flatten().also {
                if (it.isNotEmpty()) {
                    template.convertAndSend("/topic/posts/deleted", it)
                }
            }

            events.map { it.add }.flatten().also {
                if (it.isNotEmpty()) {
                    template.convertAndSend("/topic/posts/new", it)
                }
            }

            events.map { it.update }.flatten().reversed().distinct().also {
                template.convertAndSend("/topic/posts/updated", it)
            }
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

        eventBus.events.ofType(ImageEvent::class.java).buffer(Duration.ofMillis(125)).subscribe { events ->
            events.map { it.images }.flatten().reversed().distinct().groupBy { it.postId }
                .forEach { template.convertAndSend("/topic/images/${it.key}", it.value) }
        }

        eventBus.events.ofType(ThreadCreateEvent::class.java).map { it.thread }.distinct()
            .buffer(Duration.ofMillis(125)).subscribe {
                template.convertAndSend("/topic/threads", it)
            }

        eventBus.events.ofType(ThreadDeleteEvent::class.java).map { it.threadId }.distinct()
            .buffer(Duration.ofMillis(125)).subscribe {
                template.convertAndSend("/topic/threads/deleted", it)
            }

        eventBus.events.ofType(ThreadClearEvent::class.java).subscribe {
            template.convertAndSend("/topic/threads/deletedAll", listOf(true))
        }

        eventBus.events.ofType(LogCreateEvent::class.java).map { it.logEntry }.buffer(Duration.ofMillis(125))
            .subscribe { logCreateEvent ->
                logCreateEvent.distinct().also {
                    if (it.isNotEmpty()) {
                        template.convertAndSend("/topic/logs/new", it)
                    }
                }
            }

        eventBus.events.ofType(LogUpdateEvent::class.java).map { it.logEntry }.buffer(Duration.ofMillis(125))
            .subscribe { logUpdateEvent ->
                logUpdateEvent.distinct().also {
                    if (it.isNotEmpty()) {
                        template.convertAndSend("/topic/logs/updated", it)
                    }
                }
            }

        eventBus.events.ofType(LogDeleteEvent::class.java).subscribe {
            template.convertAndSend("/topic/logs/deleted", it.deleted)
        }
    }
}