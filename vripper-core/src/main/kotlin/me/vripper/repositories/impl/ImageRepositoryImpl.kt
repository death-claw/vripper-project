package me.vripper.repositories.impl

import me.vripper.entities.Image
import me.vripper.entities.domain.Status
import me.vripper.repositories.ImageRepository
import me.vripper.tables.ImageTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import java.util.*

class ImageRepositoryImpl : ImageRepository {
    override fun save(image: Image): Image {
        val id = ImageTable.insertAndGetId {
            it[downloaded] = image.downloaded
            it[host] = image.host
            it[index] = image.index
            it[postId] = image.postId
            it[status] = image.status.name
            it[size] = image.size
            it[url] = image.url
            it[thumbUrl] = image.thumbUrl
            it[postIdRef] = image.postIdRef
            it[filename] = image.filename
        }.value
        return image.copy(id = id)
    }

    override fun save(imageList: List<Image>) {
        ImageTable.batchInsert(imageList, shouldReturnGeneratedValues = false) {
            this[ImageTable.downloaded] = it.downloaded
            this[ImageTable.host] = it.host
            this[ImageTable.index] = it.index
            this[ImageTable.postId] = it.postId
            this[ImageTable.status] = it.status.name
            this[ImageTable.size] = it.size
            this[ImageTable.url] = it.url
            this[ImageTable.thumbUrl] = it.thumbUrl
            this[ImageTable.postIdRef] = it.postIdRef
            this[ImageTable.filename] = it.filename
        }
    }

    override fun deleteAllByPostId(postId: Long) {
        ImageTable.deleteWhere { ImageTable.postId eq postId }
    }

    override fun findByPostId(postId: Long): List<Image> {
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

    override fun findByPostIdAndIsNotCompleted(postId: Long): List<Image> {
        return ImageTable
            .select {
                (ImageTable.postId eq postId) and (ImageTable.status neq Status.FINISHED.name)
            }.map(this::transform)
    }

    override fun stopByPostIdAndIsNotCompleted(postId: Long): Int {
        return ImageTable.update({ (ImageTable.postId eq postId) and (ImageTable.status neq Status.FINISHED.name) }) {
            it[status] = Status.STOPPED.name
        }
    }

    override fun stopByPostIdAndIsNotCompleted(): Int {
        return ImageTable.update({ (ImageTable.status neq Status.FINISHED.name) }) {
            it[status] = Status.STOPPED.name
        }
    }

    override fun findByPostIdAndIsError(postId: Long): List<Image> {
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
            it[downloaded] = image.downloaded
            it[size] = image.size
            it[filename] = image.filename
        }
    }

    override fun update(images: List<Image>) {
        ImageTable.batchUpsert(images, shouldReturnGeneratedValues = false) {
            this[ImageTable.id] = it.id
            this[ImageTable.downloaded] = it.downloaded
            this[ImageTable.host] = it.host
            this[ImageTable.index] = it.index
            this[ImageTable.postId] = it.postId
            this[ImageTable.status] = it.status.name
            this[ImageTable.size] = it.size
            this[ImageTable.url] = it.url
            this[ImageTable.thumbUrl] = it.thumbUrl
            this[ImageTable.postIdRef] = it.postIdRef
            this[ImageTable.filename] = it.filename
        }
    }

    override fun deleteAllByPostId(postIds: List<Long>) {
        val conn = TransactionManager.current().connection.connection as Connection
        conn.prepareStatement("CREATE LOCAL TEMPORARY TABLE IMAGES_DELETE(POST_ID BIGINT PRIMARY KEY)")
            .use {
                it.execute()
            }

        conn.prepareStatement("INSERT INTO IMAGES_DELETE VALUES ( ? )").use { ps ->
            postIds.forEach {
                ps.setLong(1, it)
                ps.addBatch()
            }
            ps.executeBatch()
        }

        conn.prepareStatement("DELETE FROM IMAGE WHERE POST_ID IN (SELECT POST_ID FROM IMAGES_DELETE)")
            .use {
                it.execute()
            }

        conn.prepareStatement("TRUNCATE TABLE IMAGES_DELETE")
    }

    private fun transform(resultRow: ResultRow): Image {
        val id = resultRow[ImageTable.id].value
        val postId = resultRow[ImageTable.postId]
        val url = resultRow[ImageTable.url]
        val thumbUrl = resultRow[ImageTable.thumbUrl]
        val host = resultRow[ImageTable.host]
        val index = resultRow[ImageTable.index]
        val current = resultRow[ImageTable.downloaded]
        val total = resultRow[ImageTable.size]
        val status = Status.valueOf(resultRow[ImageTable.status])
        val postIdRef = resultRow[ImageTable.postIdRef]
        val filename = resultRow[ImageTable.filename]
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
            status,
            filename
        )
    }
}
