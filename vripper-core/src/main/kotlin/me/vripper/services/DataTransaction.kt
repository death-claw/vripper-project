package me.vripper.services

import me.vripper.data.repositories.ImageRepository
import me.vripper.data.repositories.MetadataRepository
import me.vripper.data.repositories.PostDownloadStateRepository
import me.vripper.data.repositories.ThreadRepository
import me.vripper.entities.*
import me.vripper.event.*
import me.vripper.model.ErrorCount
import me.vripper.utilities.PathUtils
import me.vripper.utilities.PathUtils.sanitize
import me.vripper.vgapi.PostItem
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString

internal class DataTransaction(
    private val settingsService: SettingsService,
    private val postDownloadStateRepository: PostDownloadStateRepository,
    private val imageRepository: ImageRepository,
    private val threadRepository: ThreadRepository,
    private val metadataRepository: MetadataRepository,
    private val eventBus: EventBus,
) {

    private val nextRank = AtomicInteger(transaction { getQueuePosition() }?.plus(1) ?: 0)

    private fun save(postEntities: List<PostEntity>): List<PostEntity> {
        return transaction { postDownloadStateRepository.save(postEntities) }
    }

    fun saveAndNotify(postEntity: PostEntity, images: List<ImageEntity>) {
        val savedPost = transaction {
            val savedPost =
                postDownloadStateRepository.save(listOf(postEntity.copy(rank = nextRank.andIncrement))).first()
            save(images.map { it.copy(postIdRef = savedPost.id) })
            savedPost
        }
        eventBus.publishEvent(PostCreateEvent(listOf(savedPost)))
    }

    fun updatePosts(postEntities: List<PostEntity>) {
        transaction { postDownloadStateRepository.update(postEntities) }
        eventBus.publishEvent(PostUpdateEvent(postEntities))
    }

    fun updatePost(postEntity: PostEntity) {
        transaction { postDownloadStateRepository.update(postEntity) }
        eventBus.publishEvent(PostUpdateEvent(listOf(postEntity)))
    }

    fun save(threadEntity: ThreadEntity) {
        val savedThread = transaction { threadRepository.save(threadEntity) }
        eventBus.publishEvent(ThreadCreateEvent(savedThread))
    }

    fun update(threadEntity: ThreadEntity) {
        transaction { threadRepository.update(threadEntity) }
        eventBus.publishEvent(ThreadUpdateEvent(threadEntity))
    }

    fun updateImages(imageEntities: List<ImageEntity>) {
        transaction { imageRepository.update(imageEntities) }
        eventBus.publishEvent(ImageEvent(imageEntities))
    }

    fun updateImage(imageEntity: ImageEntity, persist: Boolean = true) {
        if (persist) {
            transaction {
                imageRepository.update(imageEntity)
            }
        }
        eventBus.publishEvent(ImageEvent(listOf(imageEntity)))
    }

    fun exists(postId: Long): Boolean {
        return transaction { postDownloadStateRepository.existByPostId(postId) }
    }

    @Synchronized
    fun newPosts(postItems: List<PostItem>): List<PostEntity> {
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
                rank = nextRank.andIncrement,
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
        eventBus.publishEvent(PostCreateEvent(savedPosts))
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
        eventBus.publishEvent(PostDeleteEvent(postIds = postIds))
        eventBus.publishEvent(ErrorCountEvent(ErrorCount(countImagesInError())))
    }

    fun removeThread(threadId: Long) {
        transaction { threadRepository.deleteByThreadId(threadId) }
        eventBus.publishEvent(ThreadDeleteEvent(threadId))
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

    fun stopImagesByPostIdAndIsNotCompleted() {
        transaction { imageRepository.stopByPostIdAndIsNotCompleted() }
    }

    fun saveMetadata(metadataEntity: MetadataEntity) {
        transaction { metadataRepository.save(metadataEntity) }
        eventBus.publishEvent(MetadataUpdateEvent(metadataEntity))
    }

    fun clearQueueLinks() {
        transaction { threadRepository.deleteAll() }
        eventBus.publishEvent(ThreadClearEvent())
    }

    @Synchronized
    fun sortPostsByRank() {
        val postsToUpdate = mutableListOf<PostEntity>()
        val postEntities = findAllPosts().sortedWith(Comparator.comparing(PostEntity::rank))
        for (i in postEntities.indices) {
            if (postEntities[i].rank != i) {
                postsToUpdate.add(postEntities[i].copy(rank = i))
            }
        }
        updatePosts(postsToUpdate)
        nextRank.set(transaction { getQueuePosition() }?.plus(1) ?: 0)
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

    fun findAllNonCompletedPostIds(): List<Long> {
        return transaction { postDownloadStateRepository.findAllNonCompletedPostIds() }
    }

    fun findMetadataByPostId(postId: Long): Optional<MetadataEntity> {
        return transaction { metadataRepository.findByPostId(postId) }
    }
}