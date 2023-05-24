package me.mnlr.vripper.listeners

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.system.exitProcess

class AppListener : ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
        val lock = event.environment.getProperty("base.dir")?.let { Path(it).resolve("lock") }
        if(lock ==  null) {
            System.err.println("Misconfiguration detected, quitting...")
            exitProcess(-1)
        }
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