package me.mnlr.vripper.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ImageTable : LongIdTable(name = "IMAGE", columnName = "ROWID") {
    val current = long("CURRENT")
    val host = varchar("HOST", 255)
    val index = integer("INDEX")
    val postId = varchar("POST_ID", 255)
    val status = varchar("STATUS", 255)
    val total = long("TOTAL")
    val url = varchar("URL", 3000)
    val thumbUrl = varchar("THUMB_URL", 3000)
    val postIdRef = long("POST_ID_REF").references(PostTable.id, fkName = "IMAGE_POST_ID_REF_POST_ID_FK")
}