package me.vripper.utilities

import me.vripper.utilities.ApplicationProperties.VRIPPER_DIR
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.system.exitProcess

object AppLock {
    fun exclusiveLock(): Boolean {
        val lock = VRIPPER_DIR.resolve("lock")
        try {
            val randomFile = RandomAccessFile(lock.pathString, "rw")
            val channel = randomFile.channel
            val fileLock = channel.tryLock()
            if (fileLock == null) {
                return false
            } else {
                Runtime.getRuntime().addShutdownHook(Thread {
                    fileLock.release()
                    channel.close()
                    Files.deleteIfExists(lock)
                })
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(-1)
        }
    }
}