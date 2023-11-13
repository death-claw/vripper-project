package me.vripper.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object LogTable : LongIdTable(name = "LOG", columnName = "ID") {
    val type = varchar("TYPE", 8)
    val status = varchar("STATUS", 10)
    val time = datetime("TIME")
    val message = varchar("MESSAGE", 500)
}