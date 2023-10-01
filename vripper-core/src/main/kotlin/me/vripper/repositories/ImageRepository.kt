package me.vripper.repositories

import me.vripper.entities.Image
import java.util.*

interface ImageRepository {
    fun save(image: Image): Image
    fun save(imageList: List<Image>)
    fun deleteAllByPostId(postId: Long)
    fun findByPostId(postId: Long): List<Image>
    fun countError(): Int
    fun findByPostIdAndIsNotCompleted(postId: Long): List<Image>
    fun stopByPostIdAndIsNotCompleted(postId: Long): Int
    fun stopByPostIdAndIsNotCompleted(): Int
    fun findByPostIdAndIsError(postId: Long): List<Image>
    fun findById(id: Long): Optional<Image>
    fun update(image: Image)
    fun update(images: List<Image>)
    fun deleteAllByPostId(postIds: List<Long>)
}