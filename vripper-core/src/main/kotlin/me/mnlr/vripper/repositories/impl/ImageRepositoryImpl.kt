package me.mnlr.vripper.repositories.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.host.Host
import me.mnlr.vripper.repositories.ImageRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@Service
class ImageRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate, private val eventBus: EventBus
) : ImageRepository {
    @Synchronized
    private fun nextId(): Long {
        return jdbcTemplate.queryForObject("CALL NEXT VALUE FOR SEQ_IMAGE", Long::class.java)!!
    }

    override fun save(imageDownloadState: ImageDownloadState): ImageDownloadState {
        val id = nextId()
        jdbcTemplate.update(
            "INSERT INTO IMAGE (ID, CURRENT, HOST, INDEX, POST_ID, STATUS, TOTAL, URL, POST_ID_REF) VALUES (?,?,?,?,?,?,?,?,?)",
            id,
            imageDownloadState.current,
            imageDownloadState.host.host,
            imageDownloadState.index,
            imageDownloadState.postId,
            imageDownloadState.status.name,
            imageDownloadState.total,
            imageDownloadState.url,
            imageDownloadState.postIdRef
        )
        imageDownloadState.id = id
        eventBus.publishEvent(Event(Event.Kind.IMAGE_UPDATE, id))
        return imageDownloadState
    }

    override fun deleteAllByPostId(postId: String) {
        jdbcTemplate.update("DELETE FROM IMAGE WHERE POST_ID = ?", postId)
    }

    override fun findByPostId(postId: String): List<ImageDownloadState> {
        return jdbcTemplate.query(
            "SELECT * FROM IMAGE WHERE POST_ID = ?", ImageRowMapper(), postId
        )
    }

    override fun countError(): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM IMAGE AS image WHERE image.STATUS = ?", Int::class.java, Status.ERROR.name
        )
    }

    override fun findByPostIdAndIsNotCompleted(postId: String): List<ImageDownloadState> {
        return jdbcTemplate.query(
            "SELECT * FROM IMAGE AS image WHERE image.POST_ID = ? AND image.STATUS <> ?",
            ImageRowMapper(),
            postId,
            Status.FINISHED.name
        )
    }

    override fun stopByPostIdAndIsNotCompleted(postId: String): Int {
        return jdbcTemplate.update(
            "UPDATE IMAGE AS image SET image.STATUS = ? WHERE image.POST_ID = ? AND image.STATUS <> ?",
            Status.STOPPED.name,
            postId,
            Status.FINISHED.name
        )
    }

    override fun findByPostIdAndIsError(postId: String): List<ImageDownloadState> {
        return jdbcTemplate.query(
            "SELECT * FROM IMAGE AS image WHERE image.POST_ID = ? AND image.STATUS = ?",
            ImageRowMapper(),
            postId,
            Status.ERROR.name
        )
    }

    override fun findById(id: Long): Optional<ImageDownloadState> {
        val images = jdbcTemplate.query(
            "SELECT * FROM IMAGE AS image WHERE image.ID = ?", ImageRowMapper(), id
        )
        return if (images.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(images[0])
        }
    }

    override fun update(imageDownloadState: ImageDownloadState) {
        jdbcTemplate.update(
            "UPDATE IMAGE AS image SET image.STATUS = ?, image.CURRENT = ?, image.TOTAL = ? WHERE image.ID = ?",
            imageDownloadState.status.name,
            imageDownloadState.current,
            imageDownloadState.total,
            imageDownloadState.id
        )
        eventBus.publishEvent(Event(Event.Kind.IMAGE_UPDATE, imageDownloadState.id))
    }
}

internal class ImageRowMapper : RowMapper<ImageDownloadState> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): ImageDownloadState {
        val id = rs.getLong("ID")
        val postId = rs.getString("POST_ID")
        val url = rs.getString("URL")
        val host = SpringContext.getBeansOfType(Host::class.java).values.filter { it.host == rs.getString("HOST") }[0]
        val index = rs.getInt("INDEX")
        val current = rs.getLong("CURRENT")
        val total = rs.getLong("TOTAL")
        val status = Status.valueOf(rs.getString("STATUS"))
        val postIdRef = rs.getLong("POST_ID_REF")
        return ImageDownloadState(id, postId, url, host, index, postIdRef, total, current, status)
    }
}