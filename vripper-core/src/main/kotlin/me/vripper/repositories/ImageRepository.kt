package me.vripper.repositories

import me.vripper.entities.ImageEntity
import java.util.*

interface ImageRepository {
    fun save(imageEntity: ImageEntity): ImageEntity
    fun save(imageEntityList: List<ImageEntity>)
    fun deleteAllByPostId(postId: Long)
    fun findByPostId(postId: Long): List<ImageEntity>
    fun countError(): Int
    fun findByPostIdAndIsNotCompleted(postId: Long): List<ImageEntity>
    fun stopByPostIdAndIsNotCompleted(postId: Long): Int
    fun stopByPostIdAndIsNotCompleted(): Int
    fun findByPostIdAndIsError(postId: Long): List<ImageEntity>
    fun findById(id: Long): Optional<ImageEntity>
    fun update(imageEntity: ImageEntity)
    fun update(imageEntities: List<ImageEntity>)
    fun deleteAllByPostId(postIds: List<Long>)
}