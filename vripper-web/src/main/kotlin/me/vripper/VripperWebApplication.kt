package me.vripper

import me.vripper.listeners.OnStartupListener
import me.vripper.utilities.ApplicationProperties
import me.vripper.utilities.DbUtils
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class VripperWebApplication

fun main(args: Array<String>) {
    Database.connect("jdbc:h2:file:${ApplicationProperties.VRIPPER_DIR}/vripper;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000;")
    DbUtils.update()
    startKoin {
        modules(me.vripper.coreModule)
    }
    OnStartupListener().run()
    SpringApplicationBuilder(VripperWebApplication::class.java).listeners(AppListener()).run(*args)
}