package me.mnlr.vripper.services

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
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.io.path.pathString

class DataTransaction(
    private val settingsService: SettingsService,
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val imageRepository: ImageRepository,
    private val threadRepository: ThreadRepository,
    private val metadataRepository: MetadataRepository
) {

    private fun save(postDownloadState: PostDownloadState): PostDownloadState {
        return transaction { postDownloadStateRepository.save(postDownloadState) }
    }

    fun update(postDownloadState: PostDownloadState) {
        transaction { postDownloadStateRepository.update(postDownloadState) }
    }

    fun save(thread: Thread): Thread {
        return transaction { threadRepository.save(thread) }
    }

    fun update(imageDownloadState: ImageDownloadState) {
        transaction { imageRepository.update(imageDownloadState) }
    }

    fun exists(postId: String): Boolean {
        return transaction { postDownloadStateRepository.existByPostId(postId) }
    }

    fun newPost(postItem: PostItem): PostDownloadState {
        return transaction {
            val postDownloadState = save(
                PostDownloadState(
                    postTitle = postItem.title,
                    url = postItem.url,
                    token = postItem.securityToken,
                    postId = postItem.postId,
                    threadId = postItem.threadId,
                    total = postItem.imageCount,
                    hosts = postItem.hosts.map { "${it.first} (${it.second})" }.toSet(),
                    threadTitle = postItem.threadTitle,
                    forum = postItem.forum,
                    downloadDirectory = PathUtils.calculateDownloadPath(
                        postItem.forum,
                        postItem.threadTitle,
                        postItem.title,
                        postItem.postId,
                        settingsService.settings
                    ).pathString
                )
            )
            val images = postItem.imageItemList.mapIndexed { index, imageItem ->
                ImageDownloadState(
                    postId = postItem.postId,
                    url = imageItem.mainLink,
                    host = imageItem.host.hostId,
                    index = index,
                    postIdRef = postDownloadState.id!!
                )
            }
            save(images)
            sortPostsByRank()
            postDownloadState
        }
    }

    private fun save(images: List<ImageDownloadState>) {
        transaction { imageRepository.save(images) }
    }

    fun finishPost(postDownloadState: PostDownloadState) {
        val imagesInErrorStatus =
            imageRepository.findByPostIdAndIsError(postDownloadState.postId)
        if (imagesInErrorStatus.isNotEmpty()) {
            postDownloadState.status = Status.ERROR
            update(postDownloadState)
        } else {
            if (postDownloadState.done < postDownloadState.total) {
                postDownloadState.status = Status.STOPPED
                update(postDownloadState)
            } else {
                postDownloadState.status = Status.FINISHED
                transaction {
                    update(postDownloadState)
                    if (settingsService.settings.downloadSettings.clearCompleted) {
                        remove(listOf(postDownloadState.postId))
                    }
                }
            }
        }
    }

    private fun remove(postIds: List<String>) {
        transaction {
            for (postId in postIds) {
                imageRepository.deleteAllByPostId(postId)
                metadataRepository.deleteByPostId(postId)
                postDownloadStateRepository.deleteByPostId(postId)
            }
            sortPostsByRank()
        }
    }

    fun removeThread(threadId: String) {
        transaction { threadRepository.deleteByThreadId(threadId) }
    }

    fun clearCompleted(): List<String> {
        val completed = transaction { postDownloadStateRepository.findCompleted() }
        remove(completed)
        return completed
    }

    fun removeAll(postIds: List<String>?) {
        if (postIds != null) {
            remove(postIds)
        } else {
            remove(findAllPosts().map(PostDownloadState::postId))
        }
    }

    fun stopImagesByPostIdAndIsNotCompleted(postId: String) {
        transaction { imageRepository.stopByPostIdAndIsNotCompleted(postId) }
    }

    @Synchronized
    fun setMetadata(postDownloadState: PostDownloadState, metadata: Metadata) {
        if (metadataRepository.findByPostId(postDownloadState.postId).isEmpty) {
            metadata.postIdRef = postDownloadState.id
            metadataRepository.save(metadata)
        }
    }

    fun clearQueueLinks() {
        transaction { threadRepository.deleteAll() }
    }

    @Synchronized
    fun sortPostsByRank() {
        val postDownloadState = findAllPosts()
            .sortedWith(Comparator.comparing(PostDownloadState::addedOn))
        for (i in postDownloadState.indices) {
            postDownloadState[i].rank = i
        }
        update(postDownloadState)
    }

    private fun update(postDownloadStateList: List<PostDownloadState>) {
        transaction { postDownloadStateRepository.update(postDownloadStateList) }
    }

    fun setDownloadingToStopped() {
        transaction { postDownloadStateRepository.setDownloadingToStopped() }
    }

    fun findAllPosts(): List<PostDownloadState> {
        return transaction { postDownloadStateRepository.findAll() }
    }

    fun findPostsById(id: Long): Optional<PostDownloadState> {
        return transaction { postDownloadStateRepository.findById(id) }
    }

    fun findImagesByPostId(postId: String): List<ImageDownloadState> {
        return transaction { imageRepository.findByPostId(postId) }
    }

    fun findImageById(id: Long): Optional<ImageDownloadState> {
        return transaction { imageRepository.findById(id) }
    }

    fun findAllThreads(): List<Thread> {
        return transaction { threadRepository.findAll() }
    }

    fun findThreadById(id: Long): Optional<Thread> {
        return transaction {
            threadRepository.findById(id)
        }
    }
}