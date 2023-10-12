package me.mnlr.vripper.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PostTable : LongIdTable(name = "POST", columnName = "ID") {
    val done = integer("DONE")
    val hosts = varchar("HOSTS", 500)
    val outputPath = varchar("OUTPUT_PATH", 500)
    val postId = varchar("POST_ID", 255)
    val status = varchar("STATUS", 255)
    val threadId = varchar("THREAD_ID", 255)
    val postTitle = varchar("POST_TITLE", 500)
    val threadTitle = varchar("THREAD_TITLE", 500)
    val forum = varchar("FORUM", 500)
    val total = integer("TOTAL")
    val url = varchar("URL", 3000)
    val token = varchar("TOKEN", 500)
    val addedAt = datetime("ADDED_AT").default(LocalDateTime.now())
    val rank = integer("RANK").default(0)
}