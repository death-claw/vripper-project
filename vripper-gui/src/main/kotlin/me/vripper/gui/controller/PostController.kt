package me.vripper.gui.controller

import kotlinx.coroutines.*
import me.vripper.entities.Image
import me.vripper.entities.Metadata
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

    fun find(postId: Long): Deferred<PostModel> {
        return coroutineScope.async { mapper(postId) }
    }

    fun findAllPosts(): Deferred<List<PostModel>> {
        return coroutineScope.async { dataTransaction.findAllPosts().map { it.postId }.map(::mapper) }
    }

    fun mapper(postId: Long): PostModel {
        val post = dataTransaction.findPostByPostId(postId).orElseThrow()
        val metadata = dataTransaction.findMetadataByPostId(post.postId)
        return PostModel(
            post.postId,
            post.postTitle,
            progress(post.total, post.done),
            post.status.name,
            post.url,
            post.done,
            post.total,
            post.hosts.joinToString(separator = ", "),
            post.addedOn.format(dateTimeFormatter),
            post.rank + 1,
            post.getDownloadFolder(),
            post.folderName,
            progressCount(post.total, post.done, post.downloaded),
            dataTransaction.findImagesByPostId(post.postId).map(Image::thumbUrl).take(4),
            metadata.orElse(Metadata(post.postId, Metadata.Data("", emptyList())))
        )
    }

    fun progressCount(total: Int, done: Int, downloaded: Long): String {
        return "${done}/${total} (${downloaded.formatSI()})"
    }

    fun progress(total: Int, done: Int): Double {
        return if (done == 0 && total == 0) 0.0 else (done.toDouble() / total)
    }

    fun rename(postId: Long, value: String) {
        appEndpointService.rename(postId, value)
    }
}