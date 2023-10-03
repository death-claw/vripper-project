package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.tables.PostTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class PostDownloadStateRepositoryImpl :
    PostDownloadStateRepository {

    private val delimiter = ";"

    override fun save(postDownloadState: PostDownloadState): PostDownloadState {
        val id = PostTable.insertAndGetId {
            it[done] = postDownloadState.done
            it[hosts] = java.lang.String.join(delimiter, postDownloadState.hosts)
            it[outputPath] = postDownloadState.downloadDirectory
            it[postId] = postDownloadState.postId
            it[status] = postDownloadState.status.name
            it[threadId] = postDownloadState.threadId
            it[postTitle] = postDownloadState.postTitle
            it[threadTitle] = postDownloadState.threadTitle
            it[forum] = postDownloadState.forum
            it[total] = postDownloadState.total
            it[url] = postDownloadState.url
            it[token] = postDownloadState.token
            it[addedAt] = postDownloadState.addedOn
            it[rank] = postDownloadState.rank
        }.value
        return postDownloadState.copy(id = id)
    }

    override fun findByPostId(postId: String): Optional<PostDownloadState> {
        val result = PostTable.select {
            PostTable.postId eq postId
        }.map { transform(it) }
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findCompleted(): List<String> {
        return PostTable.slice(PostTable.postId).select {
            (PostTable.status eq Status.FINISHED.name) and (PostTable.done greaterEq PostTable.total)
        }.map { it[PostTable.postId] }
    }

    override fun findById(id: Long): Optional<PostDownloadState> {
        val result = PostTable.select {
            PostTable.id eq id
        }.map { transform(it) }

        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findAll(): List<PostDownloadState> {
        return PostTable.selectAll().map { transform(it) }
    }

    override fun existByPostId(postId: String): Boolean {
        return PostTable.slice(PostTable.id).selectAll().count() > 0
    }

    override fun setDownloadingToStopped(): Int {
        return PostTable.update({ (PostTable.status eq Status.DOWNLOADING.name) or (PostTable.status eq Status.PENDING.name) }) {
            it[status] = Status.STOPPED.name
        }
    }

    override fun deleteByPostId(postId: String): Int {
        return PostTable.deleteWhere { PostTable.postId eq postId }
    }

    override fun update(postDownloadState: PostDownloadState) {
        PostTable.update({ PostTable.id eq postDownloadState.id }) {
            it[status] = postDownloadState.status.name
            it[done] = postDownloadState.done
            it[rank] = postDownloadState.rank
        }
    }

    override fun update(postDownloadState: List<PostDownloadState>) {
        PostTable.batchReplace(postDownloadState, shouldReturnGeneratedValues = false) {
            this[PostTable.id] = it.id!!
            this[PostTable.status] = it.status.name
            this[PostTable.done] = it.done
            this[PostTable.total] = it.total
            this[PostTable.rank] = it.rank
            this[PostTable.hosts] = java.lang.String.join(delimiter, it.hosts)
            this[PostTable.outputPath] = it.downloadDirectory
            this[PostTable.postId] = it.postId
            this[PostTable.threadId] = it.threadId
            this[PostTable.postTitle] = it.postTitle
            this[PostTable.threadTitle] = it.threadTitle
            this[PostTable.forum] = it.forum
            this[PostTable.url] = it.url
            this[PostTable.token] = it.token
        }
    }

    private fun transform(resultRow: ResultRow): PostDownloadState {
        val id = resultRow[PostTable.id].value
        val status = Status.valueOf(resultRow[PostTable.status])
        val postId = resultRow[PostTable.postId]
        val threadId = resultRow[PostTable.threadId]
        val postTitle = resultRow[PostTable.postTitle]
        val threadTitle = resultRow[PostTable.threadTitle]
        val forum = resultRow[PostTable.forum]
        val url = resultRow[PostTable.url]
        val token = resultRow[PostTable.token]
        val done = resultRow[PostTable.done]
        val total = resultRow[PostTable.total]
        val hosts =
            resultRow[PostTable.hosts].split(delimiter).dropLastWhile { it.isEmpty() }.toSet()
        val downloadDirectory = resultRow[PostTable.outputPath]
        val addedOn = resultRow[PostTable.addedAt]
        val rank = resultRow[PostTable.rank]
        return PostDownloadState(
            id,
            postTitle,
            threadTitle,
            forum,
            url,
            token,
            postId,
            threadId,
            total,
            hosts,
            downloadDirectory,
            addedOn,
            status,
            done,
            rank
        )
    }
}
