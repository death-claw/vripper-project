package me.vripper.gui.services

import me.vripper.utilities.ApplicationProperties
import me.vripper.utilities.DbUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager

object DatabaseManager {
    private var database: Database? = null

    fun connect() {
        database =
            Database.connect("jdbc:h2:file:${ApplicationProperties.VRIPPER_DIR}/vripper;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000;")
        DbUtils.update()
    }

    fun disconnect() {
        database?.let { TransactionManager.closeAndUnregister(it) }
        database = null
    }
}