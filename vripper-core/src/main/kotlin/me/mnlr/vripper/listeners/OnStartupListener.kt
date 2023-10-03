package me.mnlr.vripper.listeners

import me.mnlr.vripper.download.DownloadService
import me.mnlr.vripper.services.DataTransaction
import me.mnlr.vripper.services.GlobalStateService
import me.mnlr.vripper.services.HTTPService
import me.mnlr.vripper.services.VGAuthService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class OnStartupListener : KoinComponent {
    private val dataTransaction: DataTransaction by inject()
    private val downloadService: DownloadService by inject()
    private val httpService: HTTPService by inject()
    private val globalStateService: GlobalStateService by inject()
    private val vgAuthService: VGAuthService by inject()

    open fun run() {
        dataTransaction.setDownloadingToStopped()
        dataTransaction.sortPostsByRank()
        httpService.init()
        vgAuthService.init()
        globalStateService.init()
        downloadService.init()
    }
}
