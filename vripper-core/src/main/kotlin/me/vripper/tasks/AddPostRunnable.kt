package me.vripper.tasks

import me.vripper.download.DownloadService
import me.vripper.model.ThreadPostId
import me.vripper.parser.PostItem
import me.vripper.parser.PostLookupAPIParser
import me.vripper.services.DataTransaction
import me.vripper.services.MetadataService
import me.vripper.services.SettingsService
import me.vripper.services.ThreadCacheService
import me.vripper.utilities.Tasks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddPostRunnable(private val items: List<ThreadPostId>) : KoinComponent, Runnable {
    private val log by me.vripper.delegate.LoggerDelegate()
    private val dataTransaction: DataTransaction by inject()
    private val settingsService: SettingsService by inject()
    private val downloadService: DownloadService by inject()
    private val threadCacheService: ThreadCacheService by inject()
    private val metadataService: MetadataService by inject()

    override fun run() {
        try {
            Tasks.increment()
            val toProcess = mutableListOf<PostItem>()
            for ((threadId, postId) in items) {
                if (dataTransaction.exists(postId)) {
                    log.warn(String.format("skipping %s, already loaded", postId))
                    continue
                }

                val threadItem = threadCacheService.getIfPresent(threadId)
                val postItem: PostItem =
                    threadItem?.postItemList?.find { it.postId == postId } ?: PostLookupAPIParser(
                        threadId,
                        postId
                    ).parse()
                toProcess.add(postItem)
            }

            val posts = dataTransaction.newPosts(toProcess.toList())
            posts.forEach {
                metadataService.fetchMetadata(it.postId)
            }
            if (settingsService.settings.downloadSettings.autoStart) {
                log.debug("Auto start downloads option is enabled")
                downloadService.restartAll(posts)
            }
        } catch (e: Exception) {
            val error = String.format("Error when adding galleries")
            log.error(error, e)
        } finally {
            Tasks.decrement()
        }
    }
}
