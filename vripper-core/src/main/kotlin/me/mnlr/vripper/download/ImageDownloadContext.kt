package me.mnlr.vripper.download

import org.apache.http.client.protocol.HttpClientContext
import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.repositories.PostDownloadStateRepository

class ImageDownloadContext(val imageId: Long, val settings: Settings) {

    private val imageRepository: ImageRepository =
        SpringContext.getBean(ImageRepository::class.java)
    private val postDownloadStateRepository: PostDownloadStateRepository =
        SpringContext.getBean(PostDownloadStateRepository::class.java)


    val httpContext: HttpClientContext = HttpClientContext.create()
    val postId = image.postIdRef
    var stopped = false
    val image: ImageDownloadState
        get() = imageRepository.findById(imageId).orElseThrow()
    val post: PostDownloadState
        get() = postDownloadStateRepository.findById(postId).orElseThrow()
}