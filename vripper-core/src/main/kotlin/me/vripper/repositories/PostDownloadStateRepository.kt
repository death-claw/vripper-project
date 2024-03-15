package me.vripper.repositories

import me.vripper.entities.PostEntity
import java.util.*

interface PostDownloadStateRepository {
    fun save(postEntities: List<PostEntity>): List<PostEntity>
    fun findByPostId(postId: Long): Optional<PostEntity>
    fun findById(id: Long): Optional<PostEntity>
    fun findCompleted(): List<Long>
    fun findAll(): List<PostEntity>
    fun existByPostId(postId: Long): Boolean
    fun setDownloadingToStopped(): Int
    fun deleteByPostId(postId: Long): Int
    fun update(postEntity: PostEntity)
    fun update(postEntities: List<PostEntity>)
    fun findMaxRank(): Int?
    fun deleteAll(postIds: List<Long>)
    fun stopAll()
    fun findAllNonCompletedPostIds(): List<Long>
}