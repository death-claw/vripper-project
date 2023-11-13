package me.vripper.repositories

import me.vripper.entities.Metadata
import java.util.*

interface MetadataRepository {
    fun save(metadata: Metadata): Metadata
    fun findByPostId(postId: Long): Optional<Metadata>
    fun deleteByPostId(postId: Long): Int
}