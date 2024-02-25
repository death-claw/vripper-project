package me.vripper.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.entities.*
import me.vripper.entities.domain.Status
import me.vripper.event.*
import me.vripper.model.ErrorCount
import me.vripper.model.PostItem
import me.vripper.repositories.*
import me.vripper.utilities.PathUtils
import me.vripper.utilities.PathUtils.sanitize
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.io.path.pathString

class DataTransaction(
    private val settingsService: SettingsService,
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val imageRepository: ImageRepository,
    private val threadRepository: ThreadRepository,
    private val metadataRepository: MetadataRepository,
    private val logRepository: LogRepository,
    private val eventBus: EventBus,
) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun save(posts: List<Post>): List<Post> {
        return transaction { postDownloadStateRepository.save(posts) }
    }

    fun updatePosts(posts: List<Post>) {
        transaction { postDownloadStateRepository.update(posts) }
        coroutineScope.launch {
            eventBus.publishEvent(PostUpdateEvent(posts))
        }
    }

    fun updatePost(post: Post) {
        transaction { postDownloadStateRepository.update(post) }
        coroutineScope.launch {
            eventBus.publishEvent(PostUpdateEvent(listOf(post)))
        }
    }

    fun save(thread: Thread) {
        val savedThread = transaction { threadRepository.save(thread) }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadCreateEvent(savedThread))
        }
    }

    fun update(thread: Thread) {
        transaction { threadRepository.update(thread) }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadUpdateEvent(thread))
        }
    }

    fun updateImages(images: List<Image>) {
        transaction { imageRepository.update(images) }
        coroutineScope.launch {
            eventBus.publishEvent(ImageEvent(images))
        }
    }

    fun updateImage(image: Image) {
        transaction { imageRepository.update(image) }
        coroutineScope.launch {
            eventBus.publishEvent(ImageEvent(listOf(image)))
        }
    }

    fun exists(postId: Long): Boolean {
        return transaction { postDownloadStateRepository.existByPostId(postId) }
    }

    @Synchronized
    fun newPosts(postItems: List<PostItem>): List<Post> {
        var queuePosition = transaction {
            getQueuePosition()?.plus(1) ?: 0
        }

        val posts = postItems.associate { postItem ->
            val post = Post(
                postTitle = postItem.title,
                url = postItem.url,
                token = postItem.securityToken,
                postId = postItem.postId,
                threadId = postItem.threadId,
                total = postItem.imageCount,
                hosts = postItem.hosts.map { "${it.first} (${it.second})" }.toSet(),
                threadTitle = postItem.threadTitle,
                forum = postItem.forum,
                rank = queuePosition++,
                downloadDirectory = PathUtils.calculateDownloadPath(
                    postItem.forum,
                    postItem.threadTitle,
                    settingsService.settings
                ).pathString,
                folderName = if (settingsService.settings.downloadSettings.appendPostId) "${sanitize(postItem.title)}_${postItem.postId}" else sanitize(
                    postItem.title
                )
            )
            val images = postItem.imageItemList.mapIndexed { index, imageItem ->
                Image(
                    postId = postItem.postId,
                    url = imageItem.mainLink,
                    thumbUrl = imageItem.thumbLink,
                    host = imageItem.host.hostId,
                    index = index,
                )
            }
            Pair(post, images)
        }


        val savedPosts = transaction {
            val savedPosts = save(posts.keys.toList())
            savedPosts.associateWith {
                posts[it]!!
            }.forEach { (key, value) ->
                save(value.map { it.copy(postIdRef = key.id) })
            }
            savedPosts
        }
        coroutineScope.launch {
            eventBus.publishEvent(PostCreateEvent(savedPosts))
        }
        return savedPosts
    }

    private fun getQueuePosition(): Int? {
        return postDownloadStateRepository.findMaxRank()
    }

    private fun save(images: List<Image>) {
        transaction { imageRepository.save(images) }
    }

    fun finishPost(postId: Long, automatic: Boolean = false) {
        val post = findPostByPostId(postId).orElseThrow()
        val imagesInErrorStatus = findByPostIdAndIsError(post.postId)
        if (imagesInErrorStatus.isNotEmpty()) {
            post.status = Status.ERROR
            updatePost(post)
        } else {
            if (post.done < post.total) {
                post.status = Status.STOPPED
                updatePost(post)
            } else {
                post.status = Status.FINISHED
                transaction {
                    updatePost(post)
                    if (settingsService.settings.downloadSettings.clearCompleted && automatic) {
                        remove(listOf(post.postId))
                    }
                }
            }
        }
    }

    private fun findByPostIdAndIsError(postId: Long): List<Image> {
        return transaction { imageRepository.findByPostIdAndIsError(postId) }

    }

    private fun remove(postIds: List<Long>) {

        transaction {
            metadataRepository.deleteAllByPostId(postIds)
            imageRepository.deleteAllByPostId(postIds)
            postDownloadStateRepository.deleteAll(postIds)
            sortPostsByRank()
        }

        coroutineScope.launch {
            eventBus.publishEvent(PostDeleteEvent(postIds = postIds))
        }
        coroutineScope.launch {
            eventBus.publishEvent(ErrorCountEvent(ErrorCount(countImagesInError())))
        }
    }

    fun removeThread(threadId: Long) {
        transaction { threadRepository.deleteByThreadId(threadId) }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadDeleteEvent(threadId))
        }
    }

    fun clearCompleted(): List<Long> {
        val completed = transaction { postDownloadStateRepository.findCompleted() }
        remove(completed)
        return completed
    }

    fun removeAll(postIds: List<Long> = emptyList()) {
        if (postIds.isNotEmpty()) {
            remove(postIds)
        } else {
            remove(findAllPosts().map(Post::postId))
        }
    }

    fun stopImagesByPostIdAndIsNotCompleted(postId: Long) {
        transaction { imageRepository.stopByPostIdAndIsNotCompleted(postId) }
    }

    fun saveMetadata(metadata: Metadata) {
        transaction { metadataRepository.save(metadata) }
        coroutineScope.launch {
            eventBus.publishEvent(MetadataUpdateEvent(metadata))
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
        val posts = findAllPosts().sortedWith(Comparator.comparing(Post::addedOn))
        for (i in posts.indices) {
            posts[i].rank = i
        }
        updatePosts(posts)
    }

    fun setDownloadingToStopped() {
        transaction { postDownloadStateRepository.setDownloadingToStopped() }
    }

    fun findAllPosts(): List<Post> {
        return transaction { postDownloadStateRepository.findAll() }
    }

    fun findPostById(id: Long): Optional<Post> {
        return transaction { postDownloadStateRepository.findById(id) }
    }

    fun findImagesByPostId(postId: Long): List<Image> {
        return transaction { imageRepository.findByPostId(postId) }
    }

    fun findImageById(id: Long): Optional<Image> {
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

    fun findByPostIdAndIsNotCompleted(postId: Long): List<Image> {
        return transaction { imageRepository.findByPostIdAndIsNotCompleted(postId) }
    }

    fun countImagesInError(): Int {
        return transaction { imageRepository.countError() }
    }

    fun findPostByPostId(postId: Long): Optional<Post> {
        return transaction { postDownloadStateRepository.findByPostId(postId) }
    }

    fun findThreadByThreadId(threadId: Long): Optional<Thread> {
        return transaction { threadRepository.findByThreadId(threadId) }
    }

    fun saveLog(logEntry: LogEntry): LogEntry {
        val pair = transaction {
            val saved = logRepository.save(logEntry)
            val deleted = logRepository.deleteOldest()
            Pair(saved, deleted)
        }
        coroutineScope.launch {
            eventBus.publishEvent(LogCreateEvent(pair.first))
        }
        coroutineScope.launch {
            eventBus.publishEvent(LogDeleteEvent(pair.second))
        }
        return pair.first
    }

    fun updateLog(logEntry: LogEntry) {
        transaction { logRepository.update(logEntry) }
        coroutineScope.launch {
            eventBus.publishEvent(LogUpdateEvent(logEntry))
        }
    }

    fun deleteAllLogs() {
        transaction { logRepository.deleteAll() }
    }

    fun findAllLogs(): List<LogEntry> {
        return transaction { logRepository.findAll() }
    }

    fun findAllNonCompletedPostIds(): List<Long> {
        return transaction { postDownloadStateRepository.findAllNonCompletedPostIds() }
    }

    fun findMetadataByPostId(postId: Long): Optional<Metadata> {
        return transaction { metadataRepository.findByPostId(postId) }
    }
}