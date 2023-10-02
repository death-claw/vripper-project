package me.mnlr.vripper.gui.controller

import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.gui.model.ImageModel
import me.mnlr.vripper.services.DataTransaction
import tornadofx.*
import java.util.*

class ImageController : Controller() {

    private val dataTransaction by di<DataTransaction>()

    fun findImages(postId: String): List<ImageModel> {
        return dataTransaction.findImagesByPostId(postId).map(::mapper)
    }

    fun findImageById(id: Long): Optional<ImageModel> {
        return dataTransaction.findImageById(id).map(::mapper)
    }

    private fun mapper(it: ImageDownloadState): ImageModel {
        return ImageModel(
            it.id!!,
            it.index + 1,
            it.url,
            if (it.current == 0L && it.total == 0L) 0.0 else (it.current.toDouble() / it.total),
            it.status.stringValue,
            it.postId
        )
    }
}