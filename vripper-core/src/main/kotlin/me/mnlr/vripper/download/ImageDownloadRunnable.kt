package me.mnlr.vripper.download

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.getExtension
import me.mnlr.vripper.host.DownloadedImage
import me.mnlr.vripper.host.Host
import me.mnlr.vripper.host.ImageMimeType
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.services.*
import net.jodah.failsafe.function.CheckedRunnable
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.BasicCookieStore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.pathString

class ImageDownloadRunnable(
    private val imageInternalId: Long, private val settings: Settings
) : KoinComponent, CheckedRunnable {
    private val log by LoggerDelegate()

    private val dataTransaction: DataTransaction by inject()
    private val hosts: List<Host> = getKoin().getAll()

    val context: ImageDownloadContext = ImageDownloadContext(imageInternalId, settings)
    private val image: Image
        get() = context.image
    private var stopped: Boolean
        get() = context.stopped
        set(value) {
            context.stopped = value
        }
    private val httpContext: HttpClientContext
        get() = context.httpContext

    init {
        httpContext.cookieStore = BasicCookieStore()
        httpContext.setAttribute(
            HTTPService.ContextAttributes.CONTEXT_ATTRIBUTES, HTTPService.ContextAttributes()
        )
    }

    @Throws(DownloadException::class)
    fun download() {
        try {
            val image = image
            with(image) {
                this.status = Status.DOWNLOADING
                this.current = 0
            }
            dataTransaction.update(image)
            synchronized(image.postId.intern()) {
                val post = context.post
                if (post.status != Status.DOWNLOADING) {
                    post.status = Status.DOWNLOADING
                    dataTransaction.update(post)
                }
            }

            if (stopped) {
                return
            }


            log.debug("Getting image url and name from ${image.url} using ${image.host}")
            val host = hosts.first { it.isSupported(image.url) }
            val downloadedImage = host.downloadInternal(image.url, context)
            log.debug("Resolved name for ${image.url}: ${downloadedImage.name}")
            log.debug(
                "Downloaded image ${image.url} to ${downloadedImage.path}"
            )
            val sanitizedFileName = PathUtils.sanitize(downloadedImage.name)
            log.debug(
                "Sanitizing image name from ${downloadedImage.name} to $sanitizedFileName"
            )
            checkImageTypeAndRename(
                context.post, downloadedImage, image.index
            )
        } catch (e: Exception) {
            if (stopped) {
                return
            }
            throw DownloadException(e)
        } finally {
            synchronized(image.postId.intern()) {
                val image = image
                if (image.current == image.total && image.total > 0) {
                    image.status = Status.FINISHED
                    val post = context.post
                    post.done += 1
                    dataTransaction.update(post)
                } else if (stopped) {
                    image.status = Status.STOPPED
                } else {
                    image.status = Status.ERROR
                }
                dataTransaction.update(image)
            }
        }
    }

    @Throws(HostException::class)
    private fun checkImageTypeAndRename(
        post: Post, downloadedImage: DownloadedImage, index: Int
    ) {
        val existingExtension = getExtension(downloadedImage.name)
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
            val downloadDestinationFolder = Path.of(post.downloadDirectory)
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
        return imageInternalId == that.imageInternalId
    }

    override fun hashCode(): Int {
        return Objects.hash(imageInternalId)
    }

    fun stop() {
        stopped = true
        val contextAttributes = httpContext.getAttribute(
            HTTPService.ContextAttributes.CONTEXT_ATTRIBUTES,
            HTTPService.ContextAttributes::class.java
        )
        if (contextAttributes != null) {
            synchronized(contextAttributes.requests) {
                for (request in contextAttributes.requests) {
                    request.abort()
                }
            }
        }
    }
}