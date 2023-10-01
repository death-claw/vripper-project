package me.mnlr.vripper.services

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.model.Settings
import java.nio.file.Path

object PathUtils {
    private val log by LoggerDelegate()

    fun calculateDownloadPath(forum: String, threadTitle: String, postTitle: String, postId: String, settings: Settings): Path {
        var downloadDirectory =
            if (settings.downloadSettings.forumSubfolder) Path.of(settings.downloadSettings.downloadPath, sanitize(forum)) else Path.of(
                settings.downloadSettings.downloadPath
            )
        downloadDirectory = if (settings.downloadSettings.threadSubLocation) downloadDirectory.resolve(threadTitle) else downloadDirectory
        downloadDirectory = downloadDirectory.resolve(if (settings.downloadSettings.appendPostId) "${sanitize(postTitle)}_${postId}" else sanitize(
            postTitle
        ))
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
}