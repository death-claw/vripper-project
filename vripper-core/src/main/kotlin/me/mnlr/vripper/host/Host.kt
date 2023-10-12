package me.mnlr.vripper.host

import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.download.ImageDownloadContext
import me.mnlr.vripper.exception.DownloadException
import me.mnlr.vripper.exception.HostException
import me.mnlr.vripper.getFileNameWithoutExtension
import me.mnlr.vripper.services.*
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.w3c.dom.Document
import java.nio.file.Files
import java.nio.file.Path

abstract class Host(
    val hostId: String,
    private val httpService: HTTPService,
    private val dataTransaction: DataTransaction,
    private val downloadSpeedService: DownloadSpeedService
) {
    private val log by LoggerDelegate()

    companion object {
        private const val READ_BUFFER_SIZE = 8192
        private val hosts: MutableList<String> = mutableListOf()

        fun getHosts(): List<String> {
            return hosts.toList()
        }
    }

    init {
        hosts.add(hostId)
    }

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
                        HtmlProcessorService.clean(it.entity.content)
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
        val mimeType = getImageMimeType(response.headers)
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
                        downloadSpeedService.reportDownloadedBytes(read.toLong())
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
        return url.contains(hostId)
    }

    @Throws(HostException::class)
    fun head(url: String, context: HttpClientContext): Array<Header> {
        val client: CloseableHttpAsyncClient = httpService.clientBuilder.build()
        val httpGet = httpService.buildHttpHead(url, context)
        log.debug(String.format("Requesting %s", url))
        return try {
            (client.execute(
                httpGet,
                context
            ) as CloseableHttpResponse).use { response ->
                try {
                    if (response.code / 100 != 2) {
                        throw HostException(
                            String.format(
                                "Unexpected response code: %d", response.code
                            )
                        )
                    }
                    response.headers
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
        val client: HttpClient = httpService.clientBuilder.build()
        val httpGet = httpService.buildHttpGet(url, context.httpContext)
        httpGet.addHeader("Referer", context.image.url)
        log.debug(String.format("Requesting %s", url))
        return try {
            (client.execute(
                httpGet,
                context.httpContext
            ) as CloseableHttpResponse).use { response ->
                if (response.code / 100 != 2) {
                    EntityUtils.consumeQuietly(response.entity)
                    throw DownloadException(
                        "Server returned code ${response.code}"
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

        if (hostId != other.hostId) return false

        return true
    }

    override fun hashCode(): Int {
        return hostId.hashCode()
    }

    override fun toString(): String {
        return hostId
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