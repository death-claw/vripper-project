package me.vripper.gui.utils

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.stage.Popup
import javafx.stage.Stage
import kotlinx.coroutines.*
import me.vripper.delegate.LoggerDelegate
import tornadofx.bind
import tornadofx.runLater
import tornadofx.sortWith
import java.io.ByteArrayInputStream

class Preview(owner: Stage, private val images: List<String>) {

    private var hBox: HBox = HBox()
    private val log by LoggerDelegate()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val previewPopup = Popup()

    init {
        val loaded: ObservableList<Pair<ImageView, Int>> = FXCollections.observableArrayList()
        hBox.children.bind(loaded) {
            it.first
        }
        previewPopup.content.add(hBox)
        previewPopup.show(owner)
        coroutineScope.launch {
            images.forEachIndexed { index, url ->
                coroutineScope.launch {
                    val imageView = previewLoading(url).await()
                    if (imageView != null) {
                        runLater {
                            loaded.add(Pair(imageView, index))
                            loaded.sortWith { o1, o2 -> o1.second.compareTo(o2.second) }
                        }
                    }
                }
            }
        }
    }

    fun hide() {
        coroutineScope.cancel()
        previewPopup.hide()
    }

    private fun previewLoading(url: String): Deferred<ImageView?> {
        return coroutineScope.async {
            try {
                ByteArrayInputStream(PreviewCache.cache[url]).use {
                    ImageView(Image(it)).apply {
                        isPreserveRatio = true

                        fitWidth = if (image.width > 200.0) {
                            if (image.width > image.height) 200.0 * image.width / image.height else 200.0
                        } else {
                            image.width
                        }
                    }
                }
            } catch (e: Exception) {
                log.warn("Failed to load preview $url")
                null
            }
        }
    }
}