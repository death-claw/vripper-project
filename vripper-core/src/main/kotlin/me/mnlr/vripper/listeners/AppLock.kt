package me.mnlr.vripper.listeners

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.system.exitProcess

class AppLock : ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
        val baseDir = event.environment.getProperty("base.dir")
        val appDirName = event.environment.getProperty("base.dir.name")
        if (baseDir == null || appDirName == null) {
            if (baseDir == null) {
                System.err.println("Property base.dir is undefined")
            }
            if (appDirName == null) {
                System.err.println("Property base.dir.name is undefined")
            }
            System.err.println("Misconfiguration detected, quitting...")
            exitProcess(-1)
        }
        val lock = Path(baseDir).resolve(appDirName).resolve("lock")
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