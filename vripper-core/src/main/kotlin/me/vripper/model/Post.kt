package me.vripper.model

import me.vripper.entities.domain.Status
import java.time.LocalDateTime
import kotlin.io.path.Path
import kotlin.io.path.pathString

data class Post(
    val id: Long = -1,
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
    val addedOn: LocalDateTime = LocalDateTime.now(),
    var folderName: String,
    var status: Status = Status.STOPPED,
    var done: Int = 0,
    var rank: Int = Int.MAX_VALUE,
    var size: Long = -1,
    var downloaded: Long = 0,
    val previews: List<String>,
    val postedBy: String,
    val resolvedNames: List<String>
) {
    fun getDownloadFolder(): String {
        return Path(downloadDirectory, folderName).pathString
    }
}
