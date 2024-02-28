package me.vripper.gui.components.fragments

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import me.vripper.entities.Metadata
import me.vripper.entities.Post
import me.vripper.event.EventBus
import me.vripper.event.PostUpdateEvent
import me.vripper.gui.controller.PostController
import me.vripper.gui.model.PostModel
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class PostInfoPanelFragment : Fragment() {
    private val postController: PostController by inject()
    private val eventBus: EventBus by di()
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val imagesTableFragment: ImagesTableFragment = find<ImagesTableFragment>()
    private val postModel: PostModel = PostModel(
        -1, "", 0.0, "", "", 0, 0, "", "", -1, "", "", "", emptyList(), Metadata(-1, Metadata.Data("", emptyList()))
    )

    override val root = tabpane()

    init {
        with(root) {
            id = "postinfo_panel"
            tabClosingPolicy = javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE
            tab("General") {
                graphic = FontIcon(Feather.INFO)
                scrollpane {
                    form {
                        fieldset {
                            field("Post Link:") {
                                textfield(postModel.urlProperty) {
                                    isEditable = false
                                    visibleWhen(postModel.urlProperty.isNotEmpty)
                                }
                            }
                            field("Posted By:") {
                                label(postModel.postedByProperty)
                            }
                            field("Title:") {
                                label(postModel.titleProperty)
                            }
                            field("More titles:") {
                                label(postModel.altTitlesProperty.map { it.joinToString(", ") })
                            }
                            field("Status:") {
                                label(postModel.statusProperty.map { it ->
                                    it.lowercase().replaceFirstChar { it.uppercase() }
                                })
                            }

                            field("Path:") {
                                label(postModel.pathProperty)
                            }
                            field("Total:") {
                                label(postModel.progressCountProperty)
                            }
                            field("Hosts:") {
                                label(postModel.hostsProperty)
                            }

                            field("Added On:") {
                                label(postModel.addedOnProperty)
                            }
                        }
                    }
                }
            }
            tab("Photos") {
                graphic = FontIcon(Feather.IMAGE)
                add(imagesTableFragment)
            }
        }
    }

    fun setPostId(postId: Long) {
        imagesTableFragment.setPostId(postId)
        coroutineScope.launch {
            val model = postController.find(postId).await()
            postModel.apply {
                this.postId = model.postId
                this.title = model.title
                this.progress = model.progress
                this.status = model.status.lowercase().replaceFirstChar { it.uppercase() }
                this.url = model.url
                this.done = model.done
                this.total = model.total
                this.hosts = model.hosts
                this.addedOn = model.addedOn
                this.order = model.order
                this.path = model.path
                this.folderName = model.folderName
                this.progressCount = model.progressCount
                this.previewList = model.previewList
                this.altTitles = model.altTitles
                this.postedby = model.postedby
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(PostUpdateEvent::class)
                .map { event -> event.posts.lastOrNull { it.postId == postId } }.collect(::updatePostModel)
        }
    }

    fun updatePostModel(post: Post?) {
        if (post == null) return
        runLater {
            postModel.status = post.status.stringValue.lowercase().replaceFirstChar { it.uppercase() }
            postModel.progressCount = postController.progressCount(
                post.total, post.done, post.downloaded
            )
            postModel.order = post.rank + 1
            postModel.done = post.done
            postModel.progress = postController.progress(
                post.total, post.done
            )
            postModel.path = post.getDownloadFolder()
            postModel.folderName = post.folderName
        }
    }

    override fun onUndock() {
        imagesTableFragment.onUndock()
        coroutineScope.cancel()
    }
}
