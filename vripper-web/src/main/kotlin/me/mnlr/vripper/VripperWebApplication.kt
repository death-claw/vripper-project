package me.mnlr.vripper

import me.mnlr.vripper.listeners.AppLock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class VripperWebApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(VripperWebApplication::class.java).listeners(AppLock()).run(*args)
}
