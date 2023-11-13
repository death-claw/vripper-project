package me.vripper.repositories.impl

import me.vripper.entities.Metadata
import me.vripper.repositories.MetadataRepository
import java.util.*

class MetadataRepositoryImpl: MetadataRepository {

    override fun save(metadata: Metadata): Metadata {
        return metadata
    }

    override fun findByPostId(postId: Long): Optional<Metadata> {
        return Optional.empty()
    }

    override fun deleteByPostId(postId: Long): Int {
        return 0
    }
}
