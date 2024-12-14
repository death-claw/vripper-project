package me.vripper.data.repositories

import me.vripper.entities.MetadataEntity
import java.util.*

internal interface MetadataRepository {
    fun save(metadataEntity: MetadataEntity): MetadataEntity
    fun findByPostId(postId: Long): Optional<MetadataEntity>
    fun deleteByPostId(postId: Long): Int
    fun deleteAllByPostId(postIds: List<Long>)
}