package me.vripper.repositories

import me.vripper.entities.Post
import java.util.*

interface PostDownloadStateRepository {
    fun save(posts: List<Post>): List<Post>
    fun findByPostId(postId: Long): Optional<Post>
    fun findById(id: Long): Optional<Post>
    fun findCompleted(): List<Long>
    fun findAll(): List<Post>
    fun existByPostId(postId: Long): Boolean
    fun setDownloadingToStopped(): Int
    fun deleteByPostId(postId: Long): Int
    fun update(post: Post)
    fun update(posts: List<Post>)
    fun findMaxRank(): Int?
    fun deleteAll(postIds: List<Long>)
    fun stopAll()
    fun findAllNonCompletedPostIds(): List<Long>
}