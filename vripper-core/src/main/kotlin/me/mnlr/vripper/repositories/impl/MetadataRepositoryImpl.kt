package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.Metadata
import me.mnlr.vripper.repositories.MetadataRepository
import java.util.*

class MetadataRepositoryImpl: MetadataRepository {

    override fun save(metadata: Metadata): Metadata {
        return metadata
    }

    override fun findByPostId(postId: String): Optional<Metadata> {
        return Optional.empty()
    }

    override fun deleteByPostId(postId: String): Int {
        return 0
    }
}
