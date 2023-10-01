package me.vripper.gui.controller

import kotlinx.coroutines.*
import me.vripper.entities.Thread
import me.vripper.gui.model.ThreadModel
import me.vripper.gui.model.ThreadSelectionModel
import me.vripper.model.ThreadPostId
import me.vripper.services.AppEndpointService
import me.vripper.services.DataTransaction
import org.koin.core.component.KoinComponent
import tornadofx.Controller

class ThreadController : KoinComponent, Controller() {

    private val dataTransaction by di<DataTransaction>()
    private val appEndpointService: AppEndpointService by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    fun delete(threadIdList: List<Long>) {
        appEndpointService.threadRemove(threadIdList)
    }

    fun clearAll() {
        appEndpointService.threadClear()
    }

    fun grab(threadId: Long): Deferred<List<ThreadSelectionModel>> = coroutineScope.async {
        appEndpointService.grab(threadId).map { postItem ->
            ThreadSelectionModel(
                postItem.number,
                postItem.title,
                postItem.url,
                postItem.hosts,
                postItem.postId,
                postItem.threadId,
                postItem.imageItemList.take(4).map { it.thumbLink }
            )
        }
    }

    fun download(selectedItems: List<ThreadSelectionModel>) {
        appEndpointService.download(selectedItems.map {
            ThreadPostId(
                it.threadId,
                it.postId
            )
        })
    }
}