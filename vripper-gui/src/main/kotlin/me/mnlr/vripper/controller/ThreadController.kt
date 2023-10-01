package me.mnlr.vripper.controller

import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.model.ThreadModel
import me.mnlr.vripper.model.ThreadSelectionModel
import me.mnlr.vripper.repositories.ThreadRepository
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import tornadofx.*
import java.util.*

class ThreadController : KoinComponent, Controller() {
    private val threadRepository: ThreadRepository by di()

    private val appEndpointService: AppEndpointService by di()

    fun findAll(): List<ThreadModel> {
        return transaction { threadRepository.findAll() } .map(::threadModelMapper)
    }

    fun find(id: Long): Optional<ThreadModel> {
        return threadRepository.findById(id).map(::threadModelMapper)
    }

    private fun threadModelMapper(it: Thread): ThreadModel {
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
                it.threadId
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