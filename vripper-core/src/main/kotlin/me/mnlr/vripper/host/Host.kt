package me.mnlr.vripper.host

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.getFileNameWithoutExtension
import me.mnlr.vripper.services.*
import org.apache.http.Header
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.util.EntityUtils
import org.w3c.dom.Document
import java.nio.file.Files
import java.nio.file.Path

abstract class Host(
    private val httpService: HTTPService,
    private val htmlProcessorService: HtmlProcessorService,
    private val dataTransaction: DataTransaction,
    private val downloadSpeedService: DownloadSpeedService
) {
    private val log by LoggerDelegate()

    companion object {
        private const val READ_BUFFER_SIZE = 8192
    }

    abstract val host: String

    @Throws(HostException::class)
    abstract fun resolve(
        url: String,
        document: Document,
        context: ImageDownloadContext
    ): Pair<String, String>

    @Throws(HostException::class)
    fun downloadInternal(url: String, context: ImageDownloadContext): DownloadedImage {
        val headers = head(url, context.httpContext)
        // is the body of type image ?
        val imageMimeType = getImageMimeType(headers)

        val downloadedImage = if (imageMimeType != null) {
            // a direct link, awesome
            val downloadedImage = fetch(url, context) {
                handleImageDownload(it, context)
            }
            DownloadedImage(getDefaultImageName(url), downloadedImage.first, downloadedImage.second)
        } else {
            // linked image ?
            val value = headers.find { it.name.contains("content-type", true) }?.value
            if (value != null) {
                if (value.contains("text/html")) {
                    val document = fetch(url, context) {
                        htmlProcessorService.clean(it.entity.content)
                    }
                    if (log.isDebugEnabled) {
                        log.debug("Cleaning $url response", url)
                    }
                    val resolvedImage = resolve(url, document, context)
                    val downloadImage: Pair<Path, ImageMimeType> =
                        fetch(resolvedImage.second, context) {
                            handleImageDownload(it, context)
                        }
                    DownloadedImage(resolvedImage.first, downloadImage.first, downloadImage.second)
                } else {
                    throw HostException("Unable to download $url, can't process content type $value")
                }
            } else {
                throw HostException("Unexpected server response for $url, response have no content type")
            }
        }
        return downloadedImage
    }

    private fun handleImageDownload(
        response: CloseableHttpResponse,
        context: ImageDownloadContext
    ): Pair<Path, ImageMimeType> {
        val mimeType = getImageMimeType(response.allHeaders)
            ?: throw HostException("Unsupported image type ${response.getFirstHeader("content-type")}")

        val tempImage = Files.createTempFile(Path.of(context.settings.downloadSettings.tempPath), "vripper_", ".tmp")
        return response.entity.content.use { inputStream ->
            try {
                Files.newOutputStream(tempImage).use { fos ->
                    val image = context.image
                    image.total = response.entity.contentLength
                    dataTransaction.update(image)
                    log.debug(
                        "Length is ${image.total}"
                    )
                    log.debug(
                        "Starting data transfer"
                    )
                    val buffer = ByteArray(READ_BUFFER_SIZE)
                    var read: Int
                    while (inputStream.read(buffer, 0, READ_BUFFER_SIZE)
                            .also { read = it } != -1 && !context.stopped
                    ) {
                        fos.write(buffer, 0, read)
                        with(image) {
                            current += read
                        }
                        downloadSpeedService.increase(read.toLong())
                        dataTransaction.update(image)
                    }
                    Pair(tempImage, mimeType)
                }
            } finally {
                EntityUtils.consumeQuietly(response.entity)
            }
        }
    }

    fun isSupported(url: String): Boolean {
        return url.contains(host)
    }

    @Throws(HostException::class)
    fun head(url: String, context: HttpClientContext): Array<Header> {
        val client: HttpClient = httpService.client.build()
        val httpGet = httpService.buildHttpHead(url, context)
        log.debug(String.format("Requesting %s", url))
        return try {
            (client.execute(
                httpGet,
                context
            ) as CloseableHttpResponse).use { response ->
                try {
                    if (response.statusLine.statusCode / 100 != 2) {
                        throw HostException(
                            String.format(
                                "Unexpected response code: %d", response.statusLine.statusCode
                            )
                        )
                    }
                    response.allHeaders
                } finally {
                    EntityUtils.consumeQuietly(response.entity)
                }
            }
        } catch (e: Exception) {
            throw HostException(e)
        }
    }

    @Throws(HostException::class)
    fun <T> fetch(
        url: String,
        context: ImageDownloadContext,
        transformer: (CloseableHttpResponse) -> T
    ): T {
        val client: HttpClient = httpService.client.build()
        val httpGet = httpService.buildHttpGet(url, context.httpContext)
        httpGet.addHeader("Referer", context.image.url)
        log.debug(String.format("Requesting %s", url))
        return try {
            (client.execute(
                httpGet,
                context.httpContext
            ) as CloseableHttpResponse).use { response ->
                if (response.statusLine.statusCode / 100 != 2) {
                    EntityUtils.consumeQuietly(response.entity)
                    throw DownloadException(
                        "Server returned code ${response.statusLine.statusCode}"
                    )
                }
                try {
                    transformer(response)
                } finally {
                    EntityUtils.consumeQuietly(response.entity)
                }
            }
        } catch (e: Exception) {
            throw HostException(e)
        }
    }

    private fun getImageMimeType(headers: Array<Header>): ImageMimeType? {

        // first check if content type header exists
        val value = headers.find { it.name.contains("content-type", true) }?.value

        // header found, check the type
        return if (value != null) {
            ImageMimeType.values().find {
                value.contains(it.strValue, true)
            }
        } else {
            null
        }
    }

    fun getDefaultImageName(imgUrl: String): String {
        val imageTitle = imgUrl.substring(imgUrl.lastIndexOf('/') + 1)
        log.debug(String.format("Extracting name from url %s: %s", imgUrl, imageTitle))
        return getFileNameWithoutExtension(imageTitle)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Host

        if (host != other.host) return false

        return true
    }

    override fun hashCode(): Int {
        return host.hashCode()
    }

    override fun toString(): String {
        return host
    }
}

data class DownloadedImage(val name: String, val path: Path, val type: ImageMimeType)

enum class ImageMimeType(val strValue: String) {
    IMAGE_BMP("image/bmp"),
    IMAGE_GIF("image/gif"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png"),
    IMAGE_WEBP("image/webp"),
}