package me.mnlr.vripper

object ApplicationProperties {
    const val BASE_DIR_NAME: String = "vripper"
    val baseDir: String = System.getProperty("base.dir", System.getProperty("user.dir"))
}
