package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.ImageDownloadState
import java.util.*

interface ImageRepository {
    fun save(imageDownloadState: ImageDownloadState): ImageDownloadState
    fun save(imageDownloadStateList: List<ImageDownloadState>)
    fun deleteAllByPostId(postId: String)
    fun findByPostId(postId: String): List<ImageDownloadState>
    fun countError(): Int
    fun findByPostIdAndIsNotCompleted(postId: String): List<ImageDownloadState>
    fun stopByPostIdAndIsNotCompleted(postId: String): Int
    fun findByPostIdAndIsError(postId: String): List<ImageDownloadState>
    fun findById(id: Long): Optional<ImageDownloadState>
    fun update(imageDownloadState: ImageDownloadState)
}