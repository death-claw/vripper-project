package me.vripper.gui.controller

import kotlinx.coroutines.*
import me.vripper.entities.Image
import me.vripper.entities.Post
import me.vripper.gui.model.PostModel
import me.vripper.services.AppEndpointService
import me.vripper.services.DataTransaction
import me.vripper.utilities.formatSI
import tornadofx.Controller
import java.time.format.DateTimeFormatter

class PostController : Controller() {

    private val dataTransaction: DataTransaction by di()
    private val appEndpointService: AppEndpointService by di()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scan(postLinks: String) {
        coroutineScope.launch {
            appEndpointService.scanLinks(postLinks)
        }
    }

    fun start(postIdList: List<Long>) {
        coroutineScope.launch {
            appEndpointService.restartAll(postIdList)
        }
    }

    fun startAll() {
        coroutineScope.launch {
            appEndpointService.restartAll()
        }
    }

    fun delete(postIdList: List<Long>) {
        coroutineScope.launch {
            appEndpointService.remove(postIdList)
        }
    }

    fun stop(postIdList: List<Long>) {
        coroutineScope.launch {
            appEndpointService.stopAll(postIdList)
        }
    }

    fun clearPosts(): Deferred<List<Long>> {
        return coroutineScope.async { appEndpointService.clearCompleted() }
    }

    fun stopAll() {
        coroutineScope.launch {
            appEndpointService.stopAll()
        }
    }

    fun findAllPosts(): Deferred<List<PostModel>> {
        return coroutineScope.async { dataTransaction.findAllPosts().map(::mapper) }
    }

    fun mapper(it: Post): PostModel {
        val updated = dataTransaction.findPostById(it.id).orElseThrow()
        return PostModel(
            updated.postId,
            updated.postTitle,
            progress(updated.total, updated.done),
            updated.status.name,
            updated.url,
            updated.done,
            updated.total,
            updated.hosts.joinToString(separator = ", "),
            updated.addedOn.format(dateTimeFormatter),
            updated.rank + 1,
            updated.downloadDirectory,
            progressCount(updated.total, updated.done, updated.downloaded),
            dataTransaction.findImagesByPostId(updated.postId).map(Image::thumbUrl).take(4)
        )
    }

    fun progressCount(total: Int, done: Int, downloaded: Long): String {
        return "${done}/${total} (${downloaded.formatSI()})"
    }

    fun progress(total: Int, done: Int): Double {
        return if (done == 0 && total == 0) 0.0 else (done.toDouble() / total)
    }
}