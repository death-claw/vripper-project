package me.mnlr.vripper.controller

import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.model.PostModel
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import tornadofx.Controller
import java.time.format.DateTimeFormatter
import java.util.*

class PostController : Controller() {

    private val postDownloadStateRepository: PostDownloadStateRepository by di()
    private val appEndpointService: AppEndpointService by di()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")

    fun scan(postLinks: String) {
        appEndpointService.scanLinks(postLinks)
    }

    fun start(postIdList: List<String>) {
        appEndpointService.restartAll(postIdList)
    }

    fun startAll() {
        appEndpointService.restartAll()
    }

    fun delete(postIdList: List<String>) {
        appEndpointService.remove(postIdList)
    }

    fun stop(postIdList: List<String>) {
        appEndpointService.stopAll(postIdList)

    }

    fun clearPosts(): List<String> {
        return appEndpointService.clearCompleted()
    }

    fun stopAll() {
        appEndpointService.stopAll(null)
    }

    fun findAllPosts(): List<PostModel> {
        return postDownloadStateRepository.findAll().map(::mapper)
    }

    fun findById(id: Long): Optional<PostModel> {
        return postDownloadStateRepository.findById(id).map(::mapper)
    }

    private fun mapper(it: PostDownloadState): PostModel {
        return PostModel(
            it.postId,
            it.postTitle,
            if (it.done == 0 && it.total == 0) 0.0 else (it.done.toDouble() / it.total),
            it.status.stringValue,
            it.url,
            it.done,
            it.total,
            it.hosts.joinToString(separator = ", "),
            it.addedOn.format(dateTimeFormatter),
            it.rank + 1,
            it.downloadDirectory,
            "${it.done}/${it.total}"
        )
    }
}