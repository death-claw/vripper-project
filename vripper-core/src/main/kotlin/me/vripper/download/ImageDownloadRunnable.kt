package me.vripper.download

import kotlinx.coroutines.*
import me.vripper.entities.Image
import me.vripper.entities.domain.Status
import me.vripper.exception.DownloadException
import me.vripper.exception.HostException
import me.vripper.host.DownloadedImage
import me.vripper.host.Host
import me.vripper.host.ImageMimeType
import me.vripper.model.Settings
import me.vripper.services.*
import me.vripper.utilities.PathUtils
import me.vripper.utilities.PathUtils.getExtension
import net.jodah.failsafe.function.CheckedRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.pathString

class ImageDownloadRunnable(
    private val image: Image, val postRank: Int, private val settings: Settings
) : KoinComponent, CheckedRunnable {
    private val log by me.vripper.delegate.LoggerDelegate()

    private val dataTransaction: DataTransaction by inject()
    private val hosts: List<Host> = getKoin().getAll()

    val context: ImageDownloadContext = ImageDownloadContext(image, settings)
    private var stopped: Boolean
        get() = context.stopped
        set(value) {
            context.stopped = value
        }

    @Throws(DownloadException::class)
    fun download() {
        try {
            image.status = Status.DOWNLOADING
            image.downloaded = 0
            dataTransaction.updateImage(image)
            val downloadDirectory: String
            synchronized(image.postId.toString().intern()) {
                val post = dataTransaction.findPostById(context.postId).orElseThrow()
                downloadDirectory = post.downloadDirectory
                if (post.status != Status.DOWNLOADING) {
                    post.status = Status.DOWNLOADING
                    dataTransaction.updatePost(post)
                }
            }
            log.debug("Getting image url and name from ${image.url} using ${image.host}")
            val host = hosts.first { it.isSupported(image.url) }
            val downloadedImage = host.downloadInternal(image.url, context)
            log.debug("Resolved name for ${image.url}: ${downloadedImage.name}")
            log.debug("Downloaded image {} to {}", image.url, downloadedImage.path)
            val sanitizedFileName = PathUtils.sanitize(downloadedImage.name)
            log.debug(
                "Sanitizing image name from ${downloadedImage.name} to $sanitizedFileName"
            )
            checkImageTypeAndRename(
                downloadDirectory, downloadedImage, image.index
            )
            synchronized(image.postId.toString().intern()) {
                if (image.downloaded == image.size && image.size > 0) {
                    image.status = Status.FINISHED
                    val post = dataTransaction.findPostById(context.postId).orElseThrow()
                    post.done += 1
                    post.downloaded += image.size
                    dataTransaction.updatePost(post)
                } else {
                    image.status = Status.ERROR
                }
                dataTransaction.updateImage(image)
            }
        } catch (e: Exception) {
            if (stopped) {
                return
            }
            image.status = Status.ERROR
            dataTransaction.updateImage(image)
            throw DownloadException(e)
        }
    }

    @Throws(HostException::class)
    private fun checkImageTypeAndRename(
        downloadDirectory: String, downloadedImage: DownloadedImage, index: Int
    ) {
        val existingExtension = getExtension(downloadedImage.name).lowercase()
        val extension = when (downloadedImage.type) {
            ImageMimeType.IMAGE_BMP -> "BMP"
            ImageMimeType.IMAGE_GIF -> "GIF"
            ImageMimeType.IMAGE_JPEG -> "JPG"
            ImageMimeType.IMAGE_PNG -> "PNG"
            ImageMimeType.IMAGE_WEBP -> "WEBP"
        }
        val filename =
            if (existingExtension.isBlank()) "${downloadedImage.name}.$extension" else downloadedImage.name
        try {
            val downloadDestinationFolder = Path.of(downloadDirectory)
            synchronized(downloadDestinationFolder.pathString.intern()) {
                Files.createDirectories(downloadDestinationFolder)
            }
            val image = downloadDestinationFolder.resolve(
                "${
                    if (settings.downloadSettings.forceOrder) String.format(
                        "%03d_", index + 1
                    ) else ""
                }$filename"
            )
            Files.copy(downloadedImage.path, image, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            throw HostException("Failed to rename the image", e)
        } finally {
            try {
                Files.delete(downloadedImage.path)
            } catch (e: IOException) {
                log.warn(
                    "Failed to delete temporary file ${downloadedImage.path}"
                )
            }
        }
    }

    @Throws(Exception::class)
    override fun run() {
        if (stopped) {
            return
        }
        download()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ImageDownloadRunnable
        return image.id == that.image.id
    }

    override fun hashCode(): Int {
        return Objects.hash(image.id)
    }

    fun stop() {
        context.requests.forEach { it.abort() }
        stopped = true
    }
}