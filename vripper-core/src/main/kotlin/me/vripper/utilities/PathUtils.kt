package me.vripper.utilities

import me.vripper.model.Settings
import java.nio.file.Path

object PathUtils {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val imageExtensions = listOf("bmp", "gif", "jpg", "jpeg", "png", "webp")


    fun calculateDownloadPath(
        forum: String, threadTitle: String, postTitle: String, postId: Long, settings: Settings
    ): Path {
        var downloadDirectory = if (settings.downloadSettings.forumSubDirectory) Path.of(
            settings.downloadSettings.downloadPath, sanitize(forum)
        ) else Path.of(
            settings.downloadSettings.downloadPath
        )
        downloadDirectory =
            if (settings.downloadSettings.threadSubLocation) downloadDirectory.resolve(threadTitle) else downloadDirectory
        downloadDirectory = downloadDirectory.resolve(
            if (settings.downloadSettings.appendPostId) "${sanitize(postTitle)}_${postId}" else sanitize(
                postTitle
            )
        )
        return downloadDirectory
    }

    /**
     * Will sanitize the image name and remove extension
     *
     * @param path
     * @return Sanitized local path string
     */
    fun sanitize(path: String): String {
        val sanitizedPath =
            path.replace("\\.|\\\\|/|\\||:|\\?|\\*|\"|<|>|\\p{Cntrl}".toRegex(), "_")
        log.debug(String.format("%s sanitized to %s", path, sanitizedPath))
        return sanitizedPath
    }

    fun getExtension(fileName: String): String {
        val extension = if (fileName.contains(".")) fileName.substring(fileName.lastIndexOf(".") + 1) else ""
        return if (!imageExtensions.contains(extension.lowercase())) {
            ""
        } else {
            extension
        }
    }

    fun getFileNameWithoutExtension(fileName: String): String {
        return if (fileName.contains(".")) fileName.substring(
            0,
            fileName.lastIndexOf(".")
        ) else fileName
    }
}