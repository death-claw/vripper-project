package me.vripper

import me.vripper.listeners.OnStartupListener
import me.vripper.utilities.DatabaseManager
import org.koin.core.context.startKoin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class VripperWebApplication

fun main(args: Array<String>) {
    DatabaseManager.connect()
    startKoin {
        modules(coreModule)
    }
    OnStartupListener().run()
    SpringApplicationBuilder(VripperWebApplication::class.java).listeners(AppListener()).run(*args)
}