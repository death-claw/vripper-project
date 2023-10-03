package me.mnlr.vripper.download

import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.services.DataTransaction
import org.apache.http.client.protocol.HttpClientContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImageDownloadContext(val imageId: Long, val settings: Settings) : KoinComponent {

    private val dataTransaction: DataTransaction by inject()

    val httpContext: HttpClientContext = HttpClientContext.create()
    val postId = image.postIdRef
    var stopped = false
    val image: ImageDownloadState
        get() = dataTransaction.findImageById(imageId).orElseThrow()
    val post: PostDownloadState
        get() = dataTransaction.findPostById(postId).orElseThrow()
}