package me.mnlr.vripper.event

import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.model.DownloadSpeed
import me.mnlr.vripper.model.ErrorCount
import me.mnlr.vripper.model.QueueState
import me.mnlr.vripper.model.Settings

data class PostCreateEvent(val post: Post)
data class PostUpdateEvent(val post: Post)
data class PostDeleteEvent(val postId: String)
data class ImageCreateEvent(val image: Image)
data class ImageUpdateEvent(val image: Image)
data class ThreadCreateEvent(val thread: Thread)
data class ThreadDeleteEvent(val threadId: String)
class ThreadClearEvent
data class VGUserLoginEvent(val username: String)
data class DownloadSpeedEvent(val downloadSpeed: DownloadSpeed)
data class QueueStateEvent(val queueState: QueueState)
data class ErrorCountEvent(val errorCount: ErrorCount)
data class SettingsUpdateEvent(val settings: Settings)