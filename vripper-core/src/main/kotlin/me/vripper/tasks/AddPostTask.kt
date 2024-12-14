package me.vripper.tasks

import kotlinx.coroutines.runBlocking
import me.vripper.download.DownloadService
import me.vripper.model.ThreadPostId
import me.vripper.services.DataTransaction
import me.vripper.services.MetadataService
import me.vripper.services.SettingsService
import me.vripper.services.ThreadCacheService
import me.vripper.utilities.LoggerDelegate
import me.vripper.vgapi.PostItem
import me.vripper.vgapi.PostLookupAPIParser
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class AddPostTask(private val items: List<ThreadPostId>) : KoinComponent, Runnable {
    private val log by LoggerDelegate()
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
                    continue
                }

                val link =
                    "https://${settingsService.settings.viperSettings.host}/threads/$threadId?p=$postId&viewfull=1#post$postId"

                val cachedThread = threadCacheService.getIfPresent(threadId)
                val threadItem = cachedThread ?: runBlocking {
                    PostLookupAPIParser(
                        threadId, postId
                    ).parse()
                }

                if (threadItem == null) {
                    log.error("Failed to load $link")
                    continue
                } else {
                    if (threadItem.error.isNotBlank()) {
                        log.error("Error loading $link: ${threadItem.error}")
                        continue
                    } else if (threadItem.postItemList.isEmpty()) {
                        log.error("Nothing found for $link")
                        continue
                    }
                }

                val postItem = threadItem.postItemList.firstOrNull { it.postId == postId }
                if (postItem == null) {
                    log.error("Unable to load $link")
                    continue
                }
                toProcess.add(postItem)
            }

            val posts = try {
                dataTransaction.newPosts(toProcess.toList())
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            posts.forEach {
                metadataService.fetchMetadata(it.postId)
            }
            if (settingsService.settings.downloadSettings.autoStart) {
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
