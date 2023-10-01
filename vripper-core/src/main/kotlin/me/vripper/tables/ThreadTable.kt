package me.vripper.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ThreadTable : LongIdTable("THREAD", columnName = "ID") {
    val total = integer("TOTAL").default(0)
    val url = varchar("URL", 200)
    val threadId = long("THREAD_ID")
    val title = varchar("TITLE", 255)
}