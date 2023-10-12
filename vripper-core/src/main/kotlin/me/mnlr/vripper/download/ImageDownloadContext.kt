package me.mnlr.vripper.download

import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.services.DataTransaction
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImageDownloadContext(val imageId: Long, val settings: Settings) : KoinComponent {

    private val dataTransaction: DataTransaction by inject()

    val httpContext: HttpClientContext = HttpClientContext.create()
    val postId = image.postIdRef
    var stopped = false
    val image: Image
        get() = dataTransaction.findImageById(imageId).orElseThrow()
    val post: Post
        get() = dataTransaction.findPostById(postId).orElseThrow()
}