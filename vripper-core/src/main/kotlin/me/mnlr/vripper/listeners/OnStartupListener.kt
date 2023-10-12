package me.mnlr.vripper.listeners

import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.services.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class OnStartupListener : KoinComponent {
    private val dataTransaction: DataTransaction by inject()
    private val downloadService: DownloadService by inject()
    private val httpService: HTTPService by inject()
    private val retryPolicyService: RetryPolicyService by inject()
    private val vgAuthService: VGAuthService by inject()
    private val downloadSpeedService: DownloadSpeedService by inject()

    open fun run() {
        dataTransaction.setDownloadingToStopped()
        dataTransaction.sortPostsByRank()
        httpService.init()
        retryPolicyService.init()
        vgAuthService.init()
        downloadService.init()
        downloadSpeedService.init()
    }
}
