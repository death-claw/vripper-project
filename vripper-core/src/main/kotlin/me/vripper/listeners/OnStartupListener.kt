package me.vripper.listeners

import me.vripper.services.DataTransaction
import me.vripper.services.MetadataService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class OnStartupListener : KoinComponent {
    private val dataTransaction: DataTransaction by inject()
    private val metadataService: MetadataService by inject()

    open fun run() {
        dataTransaction.setDownloadingToStopped()
        metadataService.init()
    }
}
