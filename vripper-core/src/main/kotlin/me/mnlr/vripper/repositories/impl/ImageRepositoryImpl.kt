package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.tables.ImageTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ImageRepositoryImpl(private val eventBus: EventBus) : ImageRepository {
    override fun save(imageDownloadState: ImageDownloadState): ImageDownloadState {

        val insertStatement = ImageTable.insert {
            it[current] = imageDownloadState.current
            it[host] = imageDownloadState.host
            it[index] = imageDownloadState.index
            it[postId] = imageDownloadState.postId
            it[status] = imageDownloadState.status.name
            it[total] = imageDownloadState.total
            it[url] = imageDownloadState.url
            it[postIdRef] = imageDownloadState.postIdRef
        }
        eventBus.publishEvent(Event(Event.Kind.IMAGE_UPDATE, insertStatement[ImageTable.id].value))
        return imageDownloadState.copy(
            id = insertStatement[ImageTable.id].value
        )
    }

    override fun deleteAllByPostId(postId: String) {
        ImageTable.deleteWhere {ImageTable.postId eq postId}
    }

    override fun findByPostId(postId: String): List<ImageDownloadState> {
        return ImageTable.select {
            ImageTable.postId eq postId
        }.map(this::transform)
    }

    override fun countError(): Int {
        return ImageTable
            .slice(ImageTable.id)
            .select { ImageTable.status eq Status.ERROR.name }
            .count().toInt()
    }

    override fun findByPostIdAndIsNotCompleted(postId: String): List<ImageDownloadState> {
        return ImageTable
            .select {
                (ImageTable.postId eq postId) and (ImageTable.status neq Status.FINISHED.name)
            }.map(this::transform)
    }

    override fun stopByPostIdAndIsNotCompleted(postId: String): Int {
        return ImageTable.update({(ImageTable.postId eq postId) and (ImageTable.status neq Status.FINISHED.name)}) {
            it[status] = Status.STOPPED.name
        }
    }

    override fun findByPostIdAndIsError(postId: String): List<ImageDownloadState> {
        return ImageTable.select {
            (ImageTable.postId eq postId) and (ImageTable.status eq Status.ERROR.name)
        }.map(this::transform)
    }

    override fun findById(id: Long): Optional<ImageDownloadState> {
        val result = ImageTable.select {
            ImageTable.id eq id
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun update(imageDownloadState: ImageDownloadState) {
        ImageTable.update({ImageTable.id eq imageDownloadState.id}) {
            it[status] = imageDownloadState.status.name
            it[current] = imageDownloadState.current
            it[total] = imageDownloadState.total
        }
        eventBus.publishEvent(Event(Event.Kind.IMAGE_UPDATE, imageDownloadState.id))
    }

    private fun transform(resultRow: ResultRow): ImageDownloadState {
        val id = resultRow[ImageTable.id].value
        val postId = resultRow[ImageTable.postId]
        val url = resultRow[ImageTable.url]
        val host = resultRow[ImageTable.host]
        val index = resultRow[ImageTable.index]
        val current = resultRow[ImageTable.current]
        val total = resultRow[ImageTable.total]
        val status = Status.valueOf(resultRow[ImageTable.status])
        val postIdRef = resultRow[ImageTable.postIdRef]
        return ImageDownloadState(id, postId, url, host, index, postIdRef, total, current, status)
    }
}
