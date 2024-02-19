package me.vripper.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ImageTable : LongIdTable(name = "IMAGE", columnName = "ID") {
    val downloaded = long("DOWNLOADED")
    val host = byte("HOST")
    val index = integer("INDEX")
    val postId = long("POST_ID")
    val status = varchar("STATUS", 15)
    val filename = varchar("FILENAME", 260)
    val size = long("SIZE")
    val url = varchar("URL", 200)
    val thumbUrl = varchar("THUMB_URL", 200)
    val postIdRef = long("POST_ID_REF").references(PostTable.id, fkName = "IMAGE_POST_ID_REF_POST_ID_FK")
}