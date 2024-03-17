package me.vripper.repositories.impl

import me.vripper.entities.ImageEntity
import me.vripper.entities.domain.Status
import me.vripper.repositories.ImageRepository
import me.vripper.tables.ImageTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import java.util.*

class ImageRepositoryImpl : ImageRepository {
    override fun save(imageEntity: ImageEntity): ImageEntity {
        val id = ImageTable.insertAndGetId {
            it[downloaded] = imageEntity.downloaded
            it[host] = imageEntity.host
            it[index] = imageEntity.index
            it[postId] = imageEntity.postId
            it[status] = imageEntity.status.name
            it[size] = imageEntity.size
            it[url] = imageEntity.url
            it[thumbUrl] = imageEntity.thumbUrl
            it[postIdRef] = imageEntity.postIdRef
            it[filename] = imageEntity.filename
        }.value
        return imageEntity.copy(id = id)
    }

    override fun save(imageEntityList: List<ImageEntity>) {
        ImageTable.batchInsert(imageEntityList, shouldReturnGeneratedValues = false) {
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

    override fun findByPostId(postId: Long): List<ImageEntity> {
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

    override fun findByPostIdAndIsNotCompleted(postId: Long): List<ImageEntity> {
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

    override fun findByPostIdAndIsError(postId: Long): List<ImageEntity> {
        return ImageTable.select {
            (ImageTable.postId eq postId) and (ImageTable.status eq Status.ERROR.name)
        }.map(this::transform)
    }

    override fun findById(id: Long): Optional<ImageEntity> {
        val result = ImageTable.select {
            ImageTable.id eq id
        }.map(this::transform)
        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    override fun update(imageEntity: ImageEntity) {
        ImageTable.update({ ImageTable.id eq imageEntity.id }) {
            it[status] = imageEntity.status.name
            it[downloaded] = imageEntity.downloaded
            it[size] = imageEntity.size
            it[filename] = imageEntity.filename
        }
    }

    override fun update(imageEntities: List<ImageEntity>) {
        ImageTable.batchUpsert(imageEntities, shouldReturnGeneratedValues = false) {
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

        conn.prepareStatement("TRUNCATE TABLE IMAGES_DELETE").use {
            it.execute()
        }
    }

    private fun transform(resultRow: ResultRow): ImageEntity {
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
        return ImageEntity(
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
