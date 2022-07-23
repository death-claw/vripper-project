package me.mnlr.vripper

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VripperWebApplication

fun main(args: Array<String>) {
    runApplication<VripperWebApplication>(*args)
}
