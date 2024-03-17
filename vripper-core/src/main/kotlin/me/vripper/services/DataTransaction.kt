package me.vripper.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.entities.*
import me.vripper.entities.domain.Status
import me.vripper.event.*
import me.vripper.model.ErrorCount
import me.vripper.parser.PostItem
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

    private fun save(postEntities: List<PostEntity>): List<PostEntity> {
        return transaction { postDownloadStateRepository.save(postEntities) }
    }

    fun updatePosts(postEntities: List<PostEntity>) {
        transaction { postDownloadStateRepository.update(postEntities) }
        coroutineScope.launch {
            eventBus.publishEvent(PostUpdateEvent(postEntities))
        }
    }

    fun updatePost(postEntity: PostEntity) {
        transaction { postDownloadStateRepository.update(postEntity) }
        coroutineScope.launch {
            eventBus.publishEvent(PostUpdateEvent(listOf(postEntity)))
        }
    }

    fun save(threadEntity: ThreadEntity) {
        val savedThread = transaction { threadRepository.save(threadEntity) }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadCreateEvent(savedThread))
        }
    }

    fun update(threadEntity: ThreadEntity) {
        transaction { threadRepository.update(threadEntity) }
        coroutineScope.launch {
            eventBus.publishEvent(ThreadUpdateEvent(threadEntity))
        }
    }

    fun updateImages(imageEntities: List<ImageEntity>) {
        transaction { imageRepository.update(imageEntities) }
        coroutineScope.launch {
            eventBus.publishEvent(ImageEvent(imageEntities))
        }
    }

    fun updateImage(imageEntity: ImageEntity) {
        transaction { imageRepository.update(imageEntity) }
        coroutineScope.launch {
            eventBus.publishEvent(ImageEvent(listOf(imageEntity)))
        }
    }

    fun exists(postId: Long): Boolean {
        return transaction { postDownloadStateRepository.existByPostId(postId) }
    }

    @Synchronized
    fun newPosts(postItems: List<PostItem>): List<PostEntity> {
        var queuePosition = transaction {
            getQueuePosition()?.plus(1) ?: 0
        }

        val posts = postItems.associate { postItem ->
            val postEntity = PostEntity(
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
            val imageEntities = postItem.imageItemList.mapIndexed { index, imageItem ->
                ImageEntity(
                    postId = postItem.postId,
                    url = imageItem.mainLink,
                    thumbUrl = imageItem.thumbLink,
                    host = imageItem.host.hostId,
                    index = index,
                )
            }
            Pair(postEntity, imageEntities)
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

    private fun save(imageEntities: List<ImageEntity>) {
        transaction { imageRepository.save(imageEntities) }
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

    private fun findByPostIdAndIsError(postId: Long): List<ImageEntity> {
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
            remove(findAllPosts().map(PostEntity::postId))
        }
    }

    fun stopImagesByPostIdAndIsNotCompleted(postId: Long) {
        transaction { imageRepository.stopByPostIdAndIsNotCompleted(postId) }
    }

    fun saveMetadata(metadataEntity: MetadataEntity) {
        transaction { metadataRepository.save(metadataEntity) }
        coroutineScope.launch {
            eventBus.publishEvent(MetadataUpdateEvent(metadataEntity))
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
        val postEntities = findAllPosts().sortedWith(Comparator.comparing(PostEntity::addedOn))
        for (i in postEntities.indices) {
            postEntities[i].rank = i
        }
        updatePosts(postEntities)
    }

    fun setDownloadingToStopped() {
        transaction { postDownloadStateRepository.setDownloadingToStopped() }
    }

    fun findAllPosts(): List<PostEntity> {
        return transaction { postDownloadStateRepository.findAll() }
    }

    fun findPostById(id: Long): Optional<PostEntity> {
        return transaction { postDownloadStateRepository.findById(id) }
    }

    fun findImagesByPostId(postId: Long): List<ImageEntity> {
        return transaction { imageRepository.findByPostId(postId) }
    }

    fun findImageById(id: Long): Optional<ImageEntity> {
        return transaction { imageRepository.findById(id) }
    }

    fun findAllThreads(): List<ThreadEntity> {
        return transaction { threadRepository.findAll() }
    }

    fun findThreadById(id: Long): Optional<ThreadEntity> {
        return transaction {
            threadRepository.findById(id)
        }
    }

    fun findByPostIdAndIsNotCompleted(postId: Long): List<ImageEntity> {
        return transaction { imageRepository.findByPostIdAndIsNotCompleted(postId) }
    }

    fun countImagesInError(): Int {
        return transaction { imageRepository.countError() }
    }

    fun findPostByPostId(postId: Long): Optional<PostEntity> {
        return transaction { postDownloadStateRepository.findByPostId(postId) }
    }

    fun findThreadByThreadId(threadId: Long): Optional<ThreadEntity> {
        return transaction { threadRepository.findByThreadId(threadId) }
    }

    fun saveLog(logEntryEntity: LogEntryEntity): LogEntryEntity {
        val pair = transaction {
            val saved = logRepository.save(logEntryEntity)
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

    fun updateLog(logEntryEntity: LogEntryEntity) {
        transaction { logRepository.update(logEntryEntity) }
        coroutineScope.launch {
            eventBus.publishEvent(LogUpdateEvent(logEntryEntity))
        }
    }

    fun deleteAllLogs() {
        transaction { logRepository.deleteAll() }
    }

    fun findAllLogs(): List<LogEntryEntity> {
        return transaction { logRepository.findAll() }
    }

    fun findAllNonCompletedPostIds(): List<Long> {
        return transaction { postDownloadStateRepository.findAllNonCompletedPostIds() }
    }

    fun findMetadataByPostId(postId: Long): Optional<MetadataEntity> {
        return transaction { metadataRepository.findByPostId(postId) }
    }
}