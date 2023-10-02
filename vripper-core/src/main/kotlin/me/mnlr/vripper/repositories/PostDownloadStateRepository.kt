package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.PostDownloadState
import java.util.*

interface PostDownloadStateRepository {
    fun save(postDownloadState: PostDownloadState): PostDownloadState
    fun findByPostId(postId: String): Optional<PostDownloadState>
    fun findById(id: Long): Optional<PostDownloadState>
    fun findCompleted(): List<String>
    fun findAll(): List<PostDownloadState>
    fun existByPostId(postId: String): Boolean
    fun setDownloadingToStopped(): Int
    fun deleteByPostId(postId: String): Int
    fun update(postDownloadState: PostDownloadState)
    fun update(postDownloadState: List<PostDownloadState>)
}