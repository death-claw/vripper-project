package me.mnlr.vripper.entities

import me.mnlr.vripper.entities.domain.Status

data class Image(
    var id: Long = -1,
    val postId: String,
    val url: String,
    val thumbUrl: String,
    val host: String,
    val index: Int,
    val postIdRef: Long,
    var total: Long = -1,
    var current: Long = 0,
    var status: Status = Status.STOPPED,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }
}