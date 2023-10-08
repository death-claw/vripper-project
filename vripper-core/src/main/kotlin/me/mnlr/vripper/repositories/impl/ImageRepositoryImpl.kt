package me.mnlr.vripper.repositories.impl

import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.tables.ImageTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ImageRepositoryImpl : ImageRepository {
    override fun save(image: Image): Image {
        val id = ImageTable.insertAndGetId {
            it[current] = image.current
            it[host] = image.host
            it[index] = image.index
            it[postId] = image.postId
            it[status] = image.status.name
            it[total] = image.total
            it[url] = image.url
            it[thumbUrl] = image.thumbUrl
            it[postIdRef] = image.postIdRef
        }.value
        return image.copy(id = id)
    }

    override fun save(imageList: List<Image>) {
        ImageTable.batchInsert(imageList, shouldReturnGeneratedValues = false) {
            this[ImageTable.current] = it.current
            this[ImageTable.host] = it.host
            this[ImageTable.index] = it.index
            this[ImageTable.postId] = it.postId
            this[ImageTable.status] = it.status.name
            this[ImageTable.total] = it.total
            this[ImageTable.url] = it.url
            this[ImageTable.thumbUrl] = it.thumbUrl
            this[ImageTable.postIdRef] = it.postIdRef
        }
    }

    override fun deleteAllByPostId(postId: String) {
        ImageTable.deleteWhere { ImageTable.postId eq postId }
    }

    override fun findByPostId(postId: String): List<Image> {
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

    override fun findByPostIdAndIsNotCompleted(postId: String): List<Image> {
        return ImageTable
            .select {
                (ImageTable.postId eq postId) and (ImageTable.status neq Status.FINISHED.name)
            }.map(this::transform)
    }

    override fun stopByPostIdAndIsNotCompleted(postId: String): Int {
        return ImageTable.update({ (ImageTable.postId eq postId) and (ImageTable.status neq Status.FINISHED.name) }) {
            it[status] = Status.STOPPED.name
        }
    }

    override fun findByPostIdAndIsError(postId: String): List<Image> {
        return ImageTable.select {
            (ImageTable.postId eq postId) and (ImageTable.status eq Status.ERROR.name)
        }.map(this::transform)
    }

    override fun findById(id: Long): Optional<Image> {
        val result = ImageTable.select {
            ImageTable.id eq id
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun update(image: Image) {
        ImageTable.update({ ImageTable.id eq image.id }) {
            it[status] = image.status.name
            it[current] = image.current
            it[total] = image.total
        }
    }

    private fun transform(resultRow: ResultRow): Image {
        val id = resultRow[ImageTable.id].value
        val postId = resultRow[ImageTable.postId]
        val url = resultRow[ImageTable.url]
        val thumbUrl = resultRow[ImageTable.thumbUrl]
        val host = resultRow[ImageTable.host]
        val index = resultRow[ImageTable.index]
        val current = resultRow[ImageTable.current]
        val total = resultRow[ImageTable.total]
        val status = Status.valueOf(resultRow[ImageTable.status])
        val postIdRef = resultRow[ImageTable.postIdRef]
        return Image(
            id,
            postId,
            url,
            thumbUrl,
            host,
            index,
            postIdRef,
            total,
            current,
            status
        )
    }
}
