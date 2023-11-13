package me.vripper.listeners

import me.vripper.utilities.ApplicationProperties.VRIPPER_DIR
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.system.exitProcess

object AppLock {
    fun exclusiveLock() {
        val lock = VRIPPER_DIR.resolve("lock")
        try {
            val randomFile = RandomAccessFile(lock.pathString, "rw")
            val channel = randomFile.channel
            val fileLock = channel.tryLock()
            if (fileLock == null) {
                System.err.println("Another instance is already running in ${lock.parent.pathString}")
                exitProcess(-1)
            } else {
                Runtime.getRuntime().addShutdownHook(Thread {
                    fileLock.release()
                    channel.close()
                    Files.deleteIfExists(lock)
                })
            }
        } catch (e: Exception) {
            println(e.toString())
        }
    }
}