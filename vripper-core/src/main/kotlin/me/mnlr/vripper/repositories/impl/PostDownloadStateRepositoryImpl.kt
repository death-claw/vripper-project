package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.tables.PostTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class PostDownloadStateRepositoryImpl :
    PostDownloadStateRepository {

    private val delimiter = ";"

    override fun save(post: Post): Post {
        val id = PostTable.insertAndGetId {
            it[done] = post.done
            it[hosts] = java.lang.String.join(delimiter, post.hosts)
            it[outputPath] = post.downloadDirectory
            it[postId] = post.postId
            it[status] = post.status.name
            it[threadId] = post.threadId
            it[postTitle] = post.postTitle
            it[threadTitle] = post.threadTitle
            it[forum] = post.forum
            it[total] = post.total
            it[url] = post.url
            it[token] = post.token
            it[addedAt] = post.addedOn
            it[rank] = post.rank
        }.value
        return post.copy(id = id)
    }

    override fun findByPostId(postId: String): Optional<Post> {
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

    override fun findById(id: Long): Optional<Post> {
        val result = PostTable.select {
            PostTable.id eq id
        }.map { transform(it) }

        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun findAll(): List<Post> {
        return PostTable.selectAll().map { transform(it) }
    }

    override fun existByPostId(postId: String): Boolean {
        return PostTable.slice(PostTable.id).select { PostTable.postId eq postId }.count() > 0
    }

    override fun setDownloadingToStopped(): Int {
        return PostTable.update({ (PostTable.status eq Status.DOWNLOADING.name) or (PostTable.status eq Status.PENDING.name) }) {
            it[status] = Status.STOPPED.name
        }
    }

    override fun deleteByPostId(postId: String): Int {
        return PostTable.deleteWhere { PostTable.postId eq postId }
    }

    override fun update(post: Post) {
        PostTable.update({ PostTable.id eq post.id }) {
            it[status] = post.status.name
            it[done] = post.done
            it[rank] = post.rank
        }
    }

    override fun update(post: List<Post>) {
        PostTable.batchReplace(post, shouldReturnGeneratedValues = false) {
            this[PostTable.id] = it.id
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

    private fun transform(resultRow: ResultRow): Post {
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
        return Post(
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
