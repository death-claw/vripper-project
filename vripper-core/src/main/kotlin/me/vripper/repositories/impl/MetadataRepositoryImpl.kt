package me.vripper.repositories.impl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.vripper.entities.MetadataEntity
import me.vripper.repositories.MetadataRepository
import me.vripper.tables.MetadataTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import java.util.*

class MetadataRepositoryImpl: MetadataRepository {

    override fun save(metadataEntity: MetadataEntity): MetadataEntity {
        MetadataTable.insert {
            it[postId] = metadataEntity.postId
            it[data] = Json.encodeToString(metadataEntity.data)
        }

        return metadataEntity
    }

    override fun findByPostId(postId: Long): Optional<MetadataEntity> {

        val result = MetadataTable.select {
            MetadataTable.postId eq postId
        }.map(::transform)

        return if (result.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(result.first())
        }
    }

    private fun transform(row: ResultRow): MetadataEntity {
        val id = row[MetadataTable.postId]
        val data = Json.decodeFromString(row[MetadataTable.data]) as MetadataEntity.Data
        return MetadataEntity(id, data)
    }

    override fun deleteByPostId(postId: Long): Int {
        return MetadataTable.deleteWhere { MetadataTable.postId eq postId }
    }

    override fun deleteAllByPostId(postIds: List<Long>) {
        val conn = TransactionManager.current().connection.connection as Connection
        conn.prepareStatement("CREATE LOCAL TEMPORARY TABLE METADATA_DELETE(POST_ID BIGINT PRIMARY KEY)")
            .use {
                it.execute()
            }

        conn.prepareStatement("INSERT INTO METADATA_DELETE VALUES ( ? )").use { ps ->
            postIds.forEach {
                ps.setLong(1, it)
                ps.addBatch()
            }
            ps.executeBatch()
        }

        conn.prepareStatement("DELETE FROM METADATA WHERE POST_ID IN (SELECT POST_ID FROM METADATA_DELETE)")
            .use {
                it.execute()
            }

        conn.prepareStatement("TRUNCATE TABLE METADATA_DELETE").use {
            it.execute()
        }
    }
}
