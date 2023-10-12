package me.mnlr.vripper.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.mnlr.vripper.entities.Image
import me.mnlr.vripper.entities.Metadata
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.entities.Thread
import me.mnlr.vripper.entities.domain.Status
import me.mnlr.vripper.event.*
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
    private val metadataRepository: MetadataRepository,
    private val eventBus: EventBus,
) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val imageIdCache: LoadingCache<Long, Optional<Image>> = Caffeine.newBuilder().build {
        _findImageById(it)
    }
    private val postIdCache: LoadingCache<Long, Optional<Post>> = Caffeine.newBuilder().build {
        _findPostById(it)
    }

    private fun save(post: Post): Post {
        return transaction { postDownloadStateRepository.save(post) }
    }

    fun update(post: Post) {
        transaction { postDownloadStateRepository.update(post) }
        postIdCache.invalidate(post.id)
        coroutineScope.launch {
            eventBus.publishEvent(PostUpdateEvent(post))
        }
    }

    fun save(thread: Thread) {
        val savedThread = transaction { threadRepository.save(thread) }
        coroutineScope.launch {
            eventBus.publishEvent(
                ThreadCreateEvent(
                    savedThread
                )
            )
        }
    }

    fun update(image: Image) {
        transaction { imageRepository.update(image) }
        imageIdCache.invalidate(image.id)
        coroutineScope.launch {
            eventBus.publishEvent(ImageUpdateEvent(image))
        }
    }

    fun exists(postId: String): Boolean {
        return transaction { postDownloadStateRepository.existByPostId(postId) }
    }

    fun newPost(postItem: PostItem): Post {
        val (post, images) = transaction {
            val post = save(
                Post(
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
                Image(
                    postId = postItem.postId,
                    url = imageItem.mainLink,
                    thumbUrl = imageItem.thumbLink,
                    host = imageItem.host.hostId,
                    index = index,
                    postIdRef = post.id!!
                )
            }
            save(images)
            sortPostsByRank()
            Pair(post, images)
        }
        coroutineScope.launch {
            eventBus.publishEvent(PostCreateEvent(post))
        }
        images.forEach {
            coroutineScope.launch {
                eventBus.publishEvent(ImageCreateEvent(it))
            }
        }
        return post
    }

    private fun save(images: List<Image>) {
        transaction { imageRepository.save(images) }
    }

    fun finishPost(post: Post) {
        val imagesInErrorStatus = findByPostIdAndIsError(post.postId)
        if (imagesInErrorStatus.isNotEmpty()) {
            post.status = Status.ERROR
            update(post)
        } else {
            if (post.done < post.total) {
                post.status = Status.STOPPED
                update(post)
            } else {
                post.status = Status.FINISHED
                transaction {
                    update(post)
                    if (settingsService.settings.downloadSettings.clearCompleted) {
                        remove(listOf(post.postId))
                    }
                }
            }
        }
    }

    private fun findByPostIdAndIsError(postId: String): List<Image> {
        return transaction { imageRepository.findByPostIdAndIsError(postId) }

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
        postIds.forEach {
            coroutineScope.launch {
                eventBus.publishEvent(PostDeleteEvent(it))
            }
        }
    }

    fun removeThread(threadId: String) {
        transaction { threadRepository.deleteByThreadId(threadId) }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadDeleteEvent(threadId))
        }
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
            remove(findAllPosts().map(Post::postId))
        }
    }

    fun stopImagesByPostIdAndIsNotCompleted(postId: String) {
        transaction { imageRepository.stopByPostIdAndIsNotCompleted(postId) }
    }

    @Synchronized
    fun setMetadata(post: Post, metadata: Metadata) {
        if (metadataRepository.findByPostId(post.postId).isEmpty) {
            metadata.postIdRef = post.id
            metadataRepository.save(metadata)
        }
    }

    fun clearQueueLinks() {
        transaction { threadRepository.deleteAll() }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadClearEvent())
        }
    }

    @Synchronized
    fun sortPostsByRank() {
        val post =
            findAllPosts().sortedWith(Comparator.comparing(Post::addedOn))
        for (i in post.indices) {
            post[i].rank = i
        }
        update(post)
    }

    private fun update(postList: List<Post>) {
        transaction { postDownloadStateRepository.update(postList) }
        postList.forEach { postIdCache.invalidate(it.id) }
        coroutineScope.launch {
            postList.forEach {
                eventBus.publishEvent(PostUpdateEvent(it))
            }
        }
    }

    fun setDownloadingToStopped() {
        transaction { postDownloadStateRepository.setDownloadingToStopped() }
    }

    fun findAllPosts(): List<Post> {
        return transaction { postDownloadStateRepository.findAll() }
    }

    fun findPostById(id: Long): Optional<Post> {
        return postIdCache[id]
    }

    private fun _findPostById(id: Long): Optional<Post> {
        return transaction { postDownloadStateRepository.findById(id) }
    }

    fun findImagesByPostId(postId: String): List<Image> {
        return transaction { imageRepository.findByPostId(postId) }
    }

    fun findImageById(id: Long): Optional<Image> {
        return imageIdCache[id]
    }

    private fun _findImageById(id: Long): Optional<Image> {
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

    fun findByPostIdAndIsNotCompleted(postId: String): List<Image> {
        return transaction { imageRepository.findByPostIdAndIsNotCompleted(postId) }
    }

    fun countImagesInError(): Int {
        return transaction { imageRepository.countError() }
    }

    fun findPostsByPostId(postId: String): Optional<Post> {
        return transaction { postDownloadStateRepository.findByPostId(postId) }
    }

    fun findThreadByThreadId(threadId: String): Optional<Thread> {
        return transaction { threadRepository.findByThreadId(threadId) }
    }
}