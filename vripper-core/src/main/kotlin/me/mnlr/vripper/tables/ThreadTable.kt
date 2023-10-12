package me.mnlr.vripper.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ThreadTable : LongIdTable("THREAD", columnName = "ID") {
    val total = integer("TOTAL").default(0)
    val url = varchar("URL", 3000)
    val threadId = varchar("THREAD_ID", 255)
    val title = varchar("TITLE", 500)
}