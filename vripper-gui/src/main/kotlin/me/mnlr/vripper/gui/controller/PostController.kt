package me.mnlr.vripper.gui.controller

import kotlinx.coroutines.*
import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.gui.model.PostModel
import me.mnlr.vripper.services.DataTransaction
import tornadofx.*
import java.time.format.DateTimeFormatter

class PostController : Controller() {

    private val dataTransaction: DataTransaction by di()
    private val appEndpointService: AppEndpointService by di()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun scan(postLinks: String) {
        coroutineScope.launch {
            appEndpointService.scanLinks(postLinks)
        }
    }

    fun start(postIdList: List<String>) {
        coroutineScope.launch {
            appEndpointService.restartAll(postIdList)
        }
    }

    fun startAll() {
        coroutineScope.launch {
            appEndpointService.restartAll()
        }
    }

    fun delete(postIdList: List<String>) {
        coroutineScope.launch {
            appEndpointService.remove(postIdList)
        }
    }

    fun stop(postIdList: List<String>) {
        coroutineScope.launch {
            appEndpointService.stopAll(postIdList)
        }
    }

    fun clearPosts(): Deferred<List<String>> {
        return coroutineScope.async { appEndpointService.clearCompleted() }
    }

    fun stopAll() {
        coroutineScope.launch {
            appEndpointService.stopAll(null)
        }
    }

    fun findAllPosts(): Deferred<List<PostModel>> {
        return coroutineScope.async { dataTransaction.findAllPosts().map(::mapper) }
    }

    fun mapper(it: Post): PostModel {
        return PostModel(
            it.postId,
            it.postTitle,
            if (it.done == 0 && it.total == 0) 0.0 else (it.done.toDouble() / it.total),
            it.status.name,
            it.url,
            it.done,
            it.total,
            it.hosts.joinToString(separator = ", "),
            it.addedOn.format(dateTimeFormatter),
            it.rank + 1,
            it.downloadDirectory,
            "${it.done}/${it.total}",
            dataTransaction.findImagesByPostId(it.postId).map(Image::thumbUrl).take(4)
        )
    }
}