package me.mnlr.vripper.gui.controller

import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.gui.model.ImageModel
import me.mnlr.vripper.services.DataTransaction
import tornadofx.*

class ImageController : Controller() {

    private val dataTransaction by di<DataTransaction>()

    fun findImages(postId: String): List<ImageModel> {
        return dataTransaction.findImagesByPostId(postId).map(::mapper)
    }

    fun mapper(it: Image): ImageModel {
        return ImageModel(
            it.id!!,
            it.index + 1,
            it.url,
            if (it.current == 0L && it.total == 0L) 0.0 else (it.current.toDouble() / it.total),
            it.status.name,
        )
    }
}