package me.mnlr.vripper.entities

import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.host.Host

data class ImageDownloadState(
    var id: Long? = null,
    val postId: String,
    val url: String,
    val host: Host,
    val index: Int,
    val postIdRef: Long,
    var total: Long = -1,
    var current: Long = 0,
    var status: Status = Status.STOPPED,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDownloadState

        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }
}