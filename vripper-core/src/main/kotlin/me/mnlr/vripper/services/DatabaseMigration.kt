package me.mnlr.vripper.services

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import me.mnlr.vripper.ApplicationProperties.BASE_DIR_NAME
import me.mnlr.vripper.ApplicationProperties.baseDir
import java.sql.DriverManager

object DatabaseMigration {

    fun update() {
        val database: Database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(JdbcConnection(DriverManager.getConnection("jdbc:h2:file:$baseDir/$BASE_DIR_NAME/vripper;DB_CLOSE_DELAY=-1;")))
        Liquibase(
            "db.changelog-master.xml",
            ClassLoaderResourceAccessor(),
            database
        ).use { liquibase ->
            liquibase.update(Contexts(), LabelExpression())
        }
    }
}