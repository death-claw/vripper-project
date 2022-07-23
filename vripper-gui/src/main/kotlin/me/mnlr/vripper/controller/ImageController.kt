package me.mnlr.vripper.controller

import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.model.ImageModel
import me.mnlr.vripper.repositories.ImageRepository
import tornadofx.Controller
import java.util.*

class ImageController : Controller() {

    private val imageRepository: ImageRepository by di()

    fun findImages(postId: String): List<ImageModel> {
        return imageRepository.findByPostId(postId).map(::mapper)
    }

    fun findImageById(id: Long): Optional<ImageModel> {
        return imageRepository.findById(id).map(::mapper)
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