package me.mnlr.vripper.listeners

import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.services.DataTransaction

@Component
class EventListenerBean(
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val dataTransaction: DataTransaction
) {
    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent?) {
        postDownloadStateRepository.setDownloadingToStopped()
        dataTransaction.sortPostsByRank()
    }
}
