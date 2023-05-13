package me.mnlr.vripper.web.restendpoints

import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.model.PostItem
import me.mnlr.vripper.web.restendpoints.domain.*
import me.mnlr.vripper.web.restendpoints.exceptions.BadRequestException
import me.mnlr.vripper.web.restendpoints.exceptions.ServerErrorException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class PostRestEndpoint(
    private val appEndpointService: AppEndpointService
) {

    private val log by LoggerDelegate()

    @PostMapping("/post")
    @ResponseStatus(code = HttpStatus.OK)
    fun scan(@RequestBody scanRequest: ScanRequest) {
        if (scanRequest.links.isNullOrBlank()) {
            log.error("Cannot process empty requests")
            throw BadRequestException("Cannot process empty requests")
        }
        appEndpointService.scanLinks(scanRequest.links)
    }

    @PostMapping("/post/restart")
    @ResponseStatus(value = HttpStatus.OK)
    fun start(@RequestBody postIds: List<String>) {
        appEndpointService.restartAll(postIds)
    }

    @PostMapping("/post/add")
    @ResponseStatus(value = HttpStatus.OK)
    fun download(@RequestBody posts: List<PostToAdd>) {
        appEndpointService.download(posts.map {
            Pair(
                it.threadId,
                it.postId
            )
        })
    }

    @PostMapping("/post/restart/all")
    @ResponseStatus(value = HttpStatus.OK)
    fun startAll() {
        appEndpointService.restartAll()
    }

    @PostMapping("/post/stop")
    @ResponseStatus(value = HttpStatus.OK)
    fun stop(@RequestBody postIds: List<String>) {
        appEndpointService.stopAll(postIds)
    }

    @PostMapping("/post/stop/all")
    @ResponseStatus(value = HttpStatus.OK)
    fun stopAll() {
        appEndpointService.stopAll(null)
    }

    @PostMapping("/post/remove")
    @ResponseStatus(value = HttpStatus.OK)
    fun remove(@RequestBody postIds: List<String>): List<String> {
        appEndpointService.remove(postIds)
        return postIds
    }

    @PostMapping("/post/clear/all")
    @ResponseStatus(value = HttpStatus.OK)
    fun clearAll(): List<String> {
        return appEndpointService.clearCompleted()
    }

    @GetMapping("/grab/{threadId}")
    @ResponseStatus(value = HttpStatus.OK)
    fun grab(@PathVariable("threadId") threadId: String): List<PostItem> {
        return try {
            appEndpointService.grab(threadId)
        } catch (e: Exception) {
            throw ServerErrorException(
                String.format(
                    "Failed to get links for threadId = %s, %s",
                    threadId,
                    e.message
                )
            )
        }
    }

    @PostMapping("/grab/remove")
    @ResponseStatus(value = HttpStatus.OK)
    fun grabRemove(@RequestBody threadIds: List<String>) {
        appEndpointService.threadRemove(threadIds)
    }

    @GetMapping("/grab/clear")
    @ResponseStatus(value = HttpStatus.OK)
    fun threadClear() {
        appEndpointService.threadClear()
    }
}