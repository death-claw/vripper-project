package me.vripper.download

import me.vripper.entities.ImageEntity
import me.vripper.model.Settings
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.koin.core.component.KoinComponent

class ImageDownloadContext(val imageEntity: ImageEntity, val settings: Settings) : KoinComponent {

    val httpContext: HttpClientContext =
        HttpClientContext.create().apply { cookieStore = BasicCookieStore() }
    val requests = mutableListOf<HttpUriRequestBase>()
    val postId = imageEntity.postIdRef
    var stopped = false
}