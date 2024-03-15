package me.vripper.web.wsendpoints

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.time.sample
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
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    @PostConstruct
    private fun run() {
        coroutineScope.launch {
            coroutineScope.launch {
                eventBus.events.filterIsInstance(PostCreateEvent::class).collect { events ->
                    template.convertAndSend("/topic/posts/new", events.postEntities)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(PostUpdateEvent::class).collect { events ->
                    template.convertAndSend("/topic/posts/updated", events.postEntities)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(PostDeleteEvent::class).collect { events ->
                    template.convertAndSend("/topic/posts/deleted", events.postIds)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(QueueStateEvent::class).collect {
                    template.convertAndSend("/topic/queue-state", it.queueState)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(DownloadSpeedEvent::class).collect {
                    template.convertAndSend("/topic/download-speed", it.downloadSpeed)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(VGUserLoginEvent::class).collect {
                    template.convertAndSend("/topic/vg-username", it.username)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(ErrorCountEvent::class).collect {
                    template.convertAndSend("/topic/error-count", it.errorCount)
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(LoadingTasks::class).sample(Duration.ofMillis(500))
                    .collect {
                        template.convertAndSend("/topic/loading", it.loading)
                    }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(ImageEvent::class).collect { events ->
                    events.imageEntities.groupBy { it.postId }.forEach {
                        template.convertAndSend("/topic/images/${it.key}", it.value)
                    }
                }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(ThreadCreateEvent::class).map { listOf(it.threadEntity) }
                    .collect { events ->
                        template.convertAndSend("/topic/threads", events)
                    }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(ThreadDeleteEvent::class)
                    .map { listOf(it.threadId) }
                    .collect {
                        template.convertAndSend("/topic/threads/deleted", it)
                    }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(ThreadClearEvent::class).map { listOf(true) }
                    .collect {
                        template.convertAndSend("/topic/threads/deletedAll", it)
                    }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(LogCreateEvent::class).map { it.logEntryEntity }
                    .collect { logCreateEvent ->
                        template.convertAndSend("/topic/logs/new", listOf(logCreateEvent))
                    }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(LogUpdateEvent::class).map { it.logEntryEntity }
                    .collect { logUpdateEvent ->
                        template.convertAndSend("/topic/logs/updated", listOf(logUpdateEvent))
                    }
            }

            coroutineScope.launch {
                eventBus.events.filterIsInstance(LogDeleteEvent::class).collect {
                    template.convertAndSend("/topic/logs/deleted", listOf(it.deleted))
                }
            }
        }
    }
}