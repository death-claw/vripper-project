package me.mnlr.vripper.gui.controller

import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.gui.model.ThreadModel
import me.mnlr.vripper.gui.model.ThreadSelectionModel
import me.mnlr.vripper.services.DataTransaction
import org.koin.core.component.KoinComponent
import tornadofx.*

class ThreadController : KoinComponent, Controller() {

    private val dataTransaction by di<DataTransaction>()
    private val appEndpointService: AppEndpointService by di()

    fun findAll(): List<ThreadModel> {
        return dataTransaction.findAllThreads().map(::threadModelMapper)
    }

    fun threadModelMapper(it: Thread): ThreadModel {
        return ThreadModel(
            it.title,
            it.link,
            it.total,
            it.threadId
        )
    }

    fun delete(threadIdList: List<String>) {
        appEndpointService.threadRemove(threadIdList)
    }

    fun clearAll() {
        appEndpointService.threadClear()
    }

    fun grab(threadId: String): List<ThreadSelectionModel> {
        return appEndpointService.grab(threadId).map {
            ThreadSelectionModel(
                it.number,
                it.title,
                it.url,
                it.hosts,
                it.postId,
                it.threadId,
                it.imageItemList.take(4).map { it.thumbLink }
            )
        }
    }

    fun download(selectedItems: List<ThreadSelectionModel>) {
        appEndpointService.download(selectedItems.map {
            Pair(
                it.threadId,
                it.postId
            )
        })
    }
}