package me.mnlr.vripper.listeners

import me.mnlr.vripper.ApplicationProperties.BASE_DIR_NAME
import me.mnlr.vripper.ApplicationProperties.baseDir
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.system.exitProcess

object AppLock {
    fun exclusiveLock() {
        val lock = Path(baseDir).resolve(BASE_DIR_NAME).resolve("lock")
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