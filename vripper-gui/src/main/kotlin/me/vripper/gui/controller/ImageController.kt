package me.vripper.gui.controller

import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.catch
import me.vripper.delegate.LoggerDelegate
import me.vripper.gui.model.ImageModel
import me.vripper.model.Image
import me.vripper.services.IAppEndpointService
import tornadofx.Controller

class ImageController : Controller() {
    private val logger by LoggerDelegate()
    lateinit var appEndpointService: IAppEndpointService
    suspend fun findImages(postId: Long): List<ImageModel> {
        return appEndpointService.findImagesByPostId(postId).map(::mapper)
    }

    private fun mapper(it: Image): ImageModel {
        return ImageModel(
            it.id,
            it.index + 1,
            it.url,
            progress(it.size, it.downloaded),
            it.status.name,
            it.size,
            it.downloaded,
            it.filename,
            it.thumbUrl
        )
    }

    fun progress(size: Long, downloaded: Long): Double {
        return if (downloaded == 0L && size == 0L) {
            0.0
        } else {
            downloaded.toDouble() / size
        }
    }

    fun onUpdateImages(postId: Long) =
        appEndpointService.onUpdateImages(postId).catch {
            logger.error("gRPC error", it)
            currentCoroutineContext().cancel(null)
        }

    fun onStopped() = appEndpointService.onStopped().catch {
        logger.error("gRPC error", it)
        currentCoroutineContext().cancel(null)
    }

}