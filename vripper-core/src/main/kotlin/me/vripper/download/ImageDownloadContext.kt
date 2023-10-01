package me.vripper.download

import me.vripper.entities.Image
import me.vripper.model.Settings
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.koin.core.component.KoinComponent

class ImageDownloadContext(val image: Image, val settings: Settings) : KoinComponent {

    val httpContext: HttpClientContext =
        HttpClientContext.create().apply { cookieStore = BasicCookieStore() }
    val requests = mutableListOf<HttpUriRequestBase>()
    val postId = image.postIdRef
    var stopped = false
}