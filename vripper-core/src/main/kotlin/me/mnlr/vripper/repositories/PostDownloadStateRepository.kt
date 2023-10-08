package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.Post
import java.util.*

interface PostDownloadStateRepository {
    fun save(post: Post): Post
    fun findByPostId(postId: String): Optional<Post>
    fun findById(id: Long): Optional<Post>
    fun findCompleted(): List<String>
    fun findAll(): List<Post>
    fun existByPostId(postId: String): Boolean
    fun setDownloadingToStopped(): Int
    fun deleteByPostId(postId: String): Int
    fun update(post: Post)
    fun update(post: List<Post>)
}