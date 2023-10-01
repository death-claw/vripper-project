package me.vripper.gui.controller

import me.vripper.entities.Image
import me.vripper.gui.model.ImageModel
import me.vripper.services.DataTransaction
import tornadofx.Controller

class ImageController : Controller() {

    private val dataTransaction by di<DataTransaction>()

    fun findImages(postId: Long): List<ImageModel> {
        return dataTransaction.findImagesByPostId(postId).map(::mapper)
    }

    private fun mapper(it: Image): ImageModel {
        return ImageModel(
            it.id,
            it.index + 1,
            it.url,
            progress(it.size, it.downloaded),
            it.status.name,
            it.size,
            it.downloaded
        )
    }

    fun progress(size: Long, downloaded: Long): Double {
        return if (downloaded == 0L && size == 0L) {
            0.0
        } else {
            downloaded.toDouble() / size
        }
    }
}