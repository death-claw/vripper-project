package me.vripper.repositories

import me.vripper.entities.MetadataEntity
import java.util.*

interface MetadataRepository {
    fun save(metadataEntity: MetadataEntity): MetadataEntity
    fun findByPostId(postId: Long): Optional<MetadataEntity>
    fun deleteByPostId(postId: Long): Int
    fun deleteAllByPostId(postIds: List<Long>)
}