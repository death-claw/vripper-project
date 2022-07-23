package me.mnlr.vripper.repositories

import me.mnlr.vripper.entities.Metadata
import java.util.*

interface MetadataRepository {
    fun save(metadata: Metadata): Metadata
    fun findByPostId(postId: String): Optional<Metadata>
    fun deleteByPostId(postId: String): Int
}