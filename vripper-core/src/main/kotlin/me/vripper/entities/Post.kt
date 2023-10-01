package me.vripper.entities

import com.fasterxml.jackson.annotation.JsonFormat
import me.vripper.entities.domain.Status
import java.time.LocalDateTime

data class Post(
    var id: Long = -1,
    val postTitle: String,
    val threadTitle: String,
    val forum: String,
    val url: String,
    val token: String,
    val postId: Long,
    val threadId: Long,
    val total: Int,
    val hosts: Set<String>,
    val downloadDirectory: String,
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") val addedOn: LocalDateTime = LocalDateTime.now(),
    var status: Status = Status.STOPPED,
    var done: Int = 0,
    var rank: Int = Int.MAX_VALUE,
    var size: Long = -1,
    var downloaded: Long = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Post

        return postId == other.postId
    }

    override fun hashCode(): Int {
        return postId.hashCode()
    }
}