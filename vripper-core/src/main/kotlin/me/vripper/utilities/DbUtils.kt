package me.vripper.utilities

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import me.vripper.utilities.ApplicationProperties.VRIPPER_DIR
import java.sql.DriverManager

object DbUtils {

    fun update() {
        val connection =
            DriverManager.getConnection("jdbc:h2:file:$VRIPPER_DIR/vripper;DB_CLOSE_DELAY=-1;")

        connection.use { cn ->
            val database: Database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(cn))
            Liquibase(
                "db.changelog-master.xml", ClassLoaderResourceAccessor(), database
            ).use { liquibase ->
                liquibase.update(Contexts(), LabelExpression())
            }
        }
    }
}