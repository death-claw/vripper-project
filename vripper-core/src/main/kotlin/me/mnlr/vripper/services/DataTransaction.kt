package me.mnlr.vripper.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import me.mnlr.vripper.entities.ImageDownloadState
import me.mnlr.vripper.entities.Metadata
import me.mnlr.vripper.entities.PostDownloadState
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.model.PostItem
import me.mnlr.vripper.repositories.ImageRepository
import me.mnlr.vripper.repositories.MetadataRepository
import me.mnlr.vripper.repositories.PostDownloadStateRepository
import me.mnlr.vripper.repositories.ThreadRepository
import kotlin.io.path.pathString

@Service
@Transactional
class DataTransaction(
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val imageRepository: ImageRepository,
    private val threadRepository: ThreadRepository,
    private val metadataRepository: MetadataRepository,
    private val settingsService: SettingsService,
    private val pathService: PathService
) {
    private fun save(postDownloadState: PostDownloadState): PostDownloadState {
        return postDownloadStateRepository.save(postDownloadState)
    }

    fun update(postDownloadState: PostDownloadState) {
        postDownloadStateRepository.update(postDownloadState)
    }

    fun save(thread: Thread): Thread {
        return threadRepository.save(thread)
    }

    private fun save(imageDownloadState: ImageDownloadState): ImageDownloadState {
        return imageRepository.save(imageDownloadState)
    }

    fun update(imageDownloadState: ImageDownloadState) {
        imageRepository.update(imageDownloadState)
    }

    fun exists(postId: String): Boolean {
        return postDownloadStateRepository.existByPostId(postId)
    }

    fun newPost(postItem: PostItem): PostDownloadState {
        val postDownloadState =  save(PostDownloadState(
            postTitle = postItem.title,
            url = postItem.url,
            token = postItem.securityToken,
            postId = postItem.postId,
            threadId = postItem.threadId,
            total = postItem.imageCount,
            hosts = postItem.hosts.keys.map { it.host }.toSet(),
            threadTitle = postItem.threadTitle,
            forum = postItem.forum,
            downloadDirectory = pathService.calculateDownloadPath(postItem.forum, postItem.threadTitle, postItem.title, postItem.postId, settingsService.settings).pathString
        ))
        val images: MutableList<ImageDownloadState> = mutableListOf()
        postItem.imageItemList.forEachIndexed { index, imageItem ->
            val imageDownloadState = ImageDownloadState(
                postId = postItem.postId,
                url = imageItem.mainLink,
                host = imageItem.host,
                index = index,
                postIdRef = postDownloadState.id!!
            )
            images.add(save(imageDownloadState))
        }
        sortPostsByRank()
        return postDownloadState
    }

    fun finishPost(postDownloadState: PostDownloadState) {
        if (imageRepository.findByPostIdAndIsError(postDownloadState.postId).isNotEmpty()) {
            postDownloadState.status = Status.ERROR
            update(postDownloadState)
        } else {
            if (postDownloadState.done < postDownloadState.total) {
                postDownloadState.status = Status.STOPPED
                update(postDownloadState)
            } else {
                postDownloadState.status = Status.FINISHED
                update(postDownloadState)
                if (settingsService.settings.downloadSettings.clearCompleted) {
                    remove(listOf(postDownloadState.postId))
                }
            }
        }
    }

    private fun remove(postIds: List<String>) {
        for (postId in postIds) {
            imageRepository.deleteAllByPostId(postId)
            metadataRepository.deleteByPostId(postId)
            postDownloadStateRepository.deleteByPostId(postId)
        }
        sortPostsByRank()
    }

    fun removeThread(threadId: String) {
        threadRepository.deleteByThreadId(threadId)
    }

    fun clearCompleted(): List<String> {
        val completed = postDownloadStateRepository.findCompleted()
        remove(completed)
        return completed
    }

    fun removeAll(postIds: List<String>?) {
        if (postIds != null) {
            remove(postIds)
        } else {
            remove(postDownloadStateRepository.findAll().map(PostDownloadState::postId))
        }
    }

    fun stopImagesByPostIdAndIsNotCompleted(postId: String) {
        imageRepository.stopByPostIdAndIsNotCompleted(postId)
    }

    @Synchronized
    fun setMetadata(postDownloadState: PostDownloadState, metadata: Metadata) {
        if (metadataRepository.findByPostId(postDownloadState.postId).isEmpty) {
            metadata.postIdRef = postDownloadState.id
            metadataRepository.save(metadata)
        }
    }

    fun clearQueueLinks() {
        threadRepository.deleteAll()
    }

    @Synchronized
    fun sortPostsByRank() {
        val postDownloadState =
            postDownloadStateRepository.findAll().sortedWith(Comparator.comparing(PostDownloadState::addedOn))
        for (i in postDownloadState.indices) {
            postDownloadState[i].rank = i
            update(postDownloadState[i])
        }
    }
}