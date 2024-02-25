package me.vripper.event

import me.vripper.entities.*
import me.vripper.model.DownloadSpeed
import me.vripper.model.ErrorCount
import me.vripper.model.QueueState
import me.vripper.model.Settings

data class PostCreateEvent(val posts: List<Post>)
data class PostUpdateEvent(val posts: List<Post>)
data class PostDeleteEvent(val postIds: List<Long>)
data class ImageEvent(val images: List<Image>)
data class ThreadCreateEvent(val thread: Thread)
data class ThreadUpdateEvent(val thread: Thread)
data class ThreadDeleteEvent(val threadId: Long)
class ThreadClearEvent
data class VGUserLoginEvent(val username: String)
data class DownloadSpeedEvent(val downloadSpeed: DownloadSpeed)
data class QueueStateEvent(val queueState: QueueState)
data class ErrorCountEvent(val errorCount: ErrorCount)
data class SettingsUpdateEvent(val settings: Settings)
data class MetadataUpdateEvent(val metadata: Metadata)

data class LogCreateEvent(val logEntry: LogEntry)
data class LogUpdateEvent(val logEntry: LogEntry)
data class LogDeleteEvent(val deleted: List<Long>)

data class LoadingTasks(val loading: Boolean)