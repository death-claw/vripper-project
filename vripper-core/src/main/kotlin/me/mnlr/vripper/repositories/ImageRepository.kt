package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.Image
import java.util.*

interface ImageRepository {
    fun save(image: Image): Image
    fun save(imageList: List<Image>)
    fun deleteAllByPostId(postId: String)
    fun findByPostId(postId: String): List<Image>
    fun countError(): Int
    fun findByPostIdAndIsNotCompleted(postId: String): List<Image>
    fun stopByPostIdAndIsNotCompleted(postId: String): Int
    fun findByPostIdAndIsError(postId: String): List<Image>
    fun findById(id: Long): Optional<Image>
    fun update(image: Image)
}