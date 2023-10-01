package me.mnlr.vripper.listeners

import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.services.DataTransaction

class OnStartup(
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val dataTransaction: DataTransaction
) {
    fun onApplicationEvent() {
        postDownloadStateRepository.setDownloadingToStopped()
        dataTransaction.sortPostsByRank()
    }
}
