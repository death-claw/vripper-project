package me.vripper.data.tables

import org.jetbrains.exposed.sql.Table

internal object MetadataTable : Table(name = "METADATA") {
    val postId = long("POST_ID")
    val data = varchar("DATA", 1_000_000)
}