package me.mnlr.vripper.repositories.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*

@Service
class PostDownloadStateRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate, private val eventBus: EventBus
) : PostDownloadStateRepository {
    @Synchronized
    private fun nextId(): Long {
        return jdbcTemplate.queryForObject("CALL NEXT VALUE FOR SEQ_POST", Long::class.java)!!
    }

    override fun save(postDownloadState: PostDownloadState): PostDownloadState {
        val id = nextId()
        jdbcTemplate.update(
            "INSERT INTO POST (ID, DONE, HOSTS, POST_FOLDER_NAME, POST_ID, STATUS, THREAD_ID, POST_TITLE, THREAD_TITLE, FORUM, TOTAL, URL, TOKEN, ADDED_ON, RANK) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id,
            postDownloadState.done,
            java.lang.String.join(";", postDownloadState.hosts),
            postDownloadState.downloadDirectory,
            postDownloadState.postId,
            postDownloadState.status.name,
            postDownloadState.threadId,
            postDownloadState.postTitle,
            postDownloadState.threadTitle,
            postDownloadState.forum,
            postDownloadState.total,
            postDownloadState.url,
            postDownloadState.token,
            Timestamp.valueOf(postDownloadState.addedOn),
            postDownloadState.rank
        )
        eventBus.publishEvent(Event(Event.Kind.POST_UPDATE, id))
        return postDownloadState.copy(id = id)
    }

    override fun findByPostId(postId: String): Optional<PostDownloadState> {
        val post = jdbcTemplate.query(
            "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID WHERE post.POST_ID = ?",
            PostRowMapper(),
            postId
        )
        return if (post.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(post[0])
        }
    }

    override fun findCompleted(): List<String> {
        return jdbcTemplate.query(
            "SELECT POST_ID FROM POST AS post WHERE status = ? AND done >= total",
            { rs: ResultSet, _: Int -> rs.getString("POST_ID") },
            Status.FINISHED.name
        )
    }

    override fun findById(id: Long): Optional<PostDownloadState> {
        val post = jdbcTemplate.query(
            "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID WHERE post.ID = ?",
            PostRowMapper(),
            id
        )
        return if (post.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(post[0])
        }
    }

    override fun findAll(): List<PostDownloadState> {
        return jdbcTemplate.query(
            "SELECT metadata.*,post.* FROM METADATA metadata FULL JOIN POST post ON metadata.POST_ID_REF = post.ID",
            PostRowMapper()
        )
    }

    override fun existByPostId(postId: String): Boolean {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM POST AS post WHERE post.POST_ID = ?", Int::class.java, postId
        )
        return count > 0
    }

    override fun setDownloadingToStopped(): Int {
        return jdbcTemplate.update(
            "UPDATE POST AS post SET post.STATUS = ? WHERE post.STATUS = ? OR post.STATUS = ?",
            Status.STOPPED.name,
            Status.DOWNLOADING.name,
            Status.PENDING.name
        )
    }

    override fun deleteByPostId(postId: String): Int {
        val mutationCount = jdbcTemplate.update("DELETE FROM POST AS post WHERE post.POST_ID = ?", postId)
        eventBus.publishEvent(Event(Event.Kind.POST_REMOVE, postId))
        return mutationCount
    }

    override fun update(postDownloadState: PostDownloadState) {
        jdbcTemplate.update(
            "UPDATE POST AS post SET post.STATUS = ?, post.DONE = ?, post.RANK = ? WHERE post.ID = ?",
            postDownloadState.status.name,
            postDownloadState.done,
            postDownloadState.rank,
            postDownloadState.id
        )
        eventBus.publishEvent(Event(Event.Kind.POST_UPDATE, postDownloadState.id))
    }
}

internal class PostRowMapper : RowMapper<PostDownloadState> {
    @Throws(SQLException::class)
    override fun mapRow(rs: ResultSet, rowNum: Int): PostDownloadState {
        val id = rs.getLong("post.ID")
        val status = Status.valueOf(rs.getString("post.STATUS"))
        val postId = rs.getString("post.POST_ID")
        val threadId = rs.getString("post.THREAD_ID")
        val postTitle = rs.getString("post.POST_TITLE")
        val threadTitle = rs.getString("post.THREAD_TITLE")
        val forum = rs.getString("post.FORUM")
        val url = rs.getString("post.URL")
        val token = rs.getString("post.TOKEN")
        val done = rs.getInt("post.DONE")
        val total = rs.getInt("post.TOTAL")
        val hosts = rs.getString("post.HOSTS").split(DELIMITER).dropLastWhile { it.isEmpty() }.toSet()
        val downloadDirectory = rs.getString("post.POST_FOLDER_NAME")
        val addedOn = rs.getTimestamp("post.ADDED_ON").toLocalDateTime()
        val rank = rs.getInt("post.RANK")
//        val metadataId = rs.getLong("metadata.POST_ID_REF")
//        if (!rs.wasNull()) {
//            val metadata = Metadata()
//            metadata.postIdRef = metadataId
//            metadata.postId = rs.getString("metadata.POST_ID")
//            val resolvedNames = rs.getString("metadata.RESOLVED_NAMES")
//            if (resolvedNames != null && resolvedNames.isNotBlank()) {
//                metadata.resolvedNames =
//                    resolvedNames.split("%sep%").dropLastWhile { it.isEmpty() }
//            }
//            metadata.postedBy = rs.getString("metadata.POSTED_BY")
//            postDownloadState.metadata = metadata
//        }
        return PostDownloadState(
            id, postTitle, threadTitle, forum, url, token, postId, threadId, total, hosts, downloadDirectory, addedOn, status, done, rank
        )
    }

    companion object {
        private const val DELIMITER = ";"
    }
}