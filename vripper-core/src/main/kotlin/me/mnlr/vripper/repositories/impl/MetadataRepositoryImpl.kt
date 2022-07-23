package me.mnlr.vripper.repositories.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import me.mnlr.vripper.entities.Metadata
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.repositories.*
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@Service
class MetadataRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val eventBus: EventBus
) : MetadataRepository {
    override fun save(metadata: Metadata): Metadata {
        jdbcTemplate.update(
            "INSERT INTO METADATA (POST_ID_REF, POST_ID, POSTED_BY, RESOLVED_NAMES) VALUES (?,?,?,?)",
            metadata.postIdRef,
            metadata.postId,
            metadata.postedBy,
            java.lang.String.join("%sep%", metadata.resolvedNames)
        )
        eventBus.publishEvent(Event(Event.Kind.METADATA_UPDATE, metadata.postIdRef))
        return metadata
    }

    override fun findByPostId(postId: String): Optional<Metadata> {
        val metadata = jdbcTemplate.query(
            "SELECT metadata.* FROM METADATA AS metadata WHERE metadata.POST_ID = ?",
            MetadataRowMapper(),
            postId
        )
        return if (metadata.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(metadata[0])
        }
    }

    override fun deleteByPostId(postId: String): Int {
        return jdbcTemplate.update(
            "DELETE FROM METADATA AS metadata WHERE metadata.POST_ID = ?", postId
        )
    }
}

internal class MetadataRowMapper : RowMapper<Metadata> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): Metadata {
        val metadata = Metadata()
        metadata.postIdRef = rs.getLong("POST_ID_REF")
        metadata.postId = rs.getString("POST_ID")
        metadata.postedBy = rs.getString("POSTED_BY")
        val resolvedNames = rs.getString("RESOLVED_NAMES")
        if (resolvedNames != null && resolvedNames.isNotBlank()) {
            metadata.resolvedNames =
                resolvedNames.split("%sep%").dropLastWhile { it.isEmpty() }
        }
        return metadata
    }
}