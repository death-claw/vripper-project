package me.vripper.entities

import me.vripper.entities.domain.Status

data class Image(
    var id: Long = -1,
    val postId: Long,
    val url: String,
    val thumbUrl: String,
    val host: Byte,
    val index: Int,
    val postIdRef: Long = -1,
    var size: Long = -1,
    var downloaded: Long = 0,
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