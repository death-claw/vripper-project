package me.mnlr.vripper

import java.nio.file.Files
import java.nio.file.Path

object ApplicationProperties {
    const val BASE_DIR_NAME: String = "vripper"
    val baseDir: String = System.getProperty("base.dir", System.getProperty("user.dir"))

    init {
        Files.createDirectories(Path.of(baseDir, BASE_DIR_NAME))
    }
}
