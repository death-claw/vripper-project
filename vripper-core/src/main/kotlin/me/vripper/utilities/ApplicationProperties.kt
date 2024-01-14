package me.vripper.utilities

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

object ApplicationProperties {
    const val VERSION: String = "5.2.0"
    private const val BASE_DIR_NAME: String = "vripper"
    private val portable = System.getProperty("vripper.portable", "true").toBoolean()
    private val BASE_DIR: String = getBaseDir()
    val VRIPPER_DIR: Path = Path(BASE_DIR, BASE_DIR_NAME)

    init {
        Files.createDirectories(VRIPPER_DIR)
        System.setProperty("VRIPPER_DIR", VRIPPER_DIR.toRealPath().pathString)
    }

    private fun getBaseDir(): String {
        return if (portable) {
            System.getProperty("user.dir")
        } else {
            val os = System.getProperty("os.name")
            if (os.contains("Windows")) {
                System.getProperty("user.home")
            } else if (os.contains("Linux")) {
                "${System.getProperty("user.home")}/.config"
            } else if (os.contains("Mac")) {
                "${System.getProperty("user.home")}/.config"
            } else {
                System.getProperty("user.dir")
            }
        }
    }
}
