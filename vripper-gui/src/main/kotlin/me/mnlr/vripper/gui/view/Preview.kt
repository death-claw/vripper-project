package me.mnlr.vripper.gui.view

import javafx.scene.control.ProgressIndicator
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.stage.Popup
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.asFlow
import me.mnlr.vripper.gui.view.PreviewCache.previewDispatcher
import tornadofx.*
import java.io.ByteArrayInputStream

class Preview(private val owner: Stage, val images: List<String>) {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var previewLoadJob: Job? = null
    val previewPopup = Popup()

    init {
        previewPopup.content.add(ProgressIndicator().apply { prefWidth = 25.0; prefHeight = 25.0 })
        previewPopup.show(owner)
        show()
    }

    fun hide() {
        previewLoadJob?.cancel()
        coroutineScope.cancel()
        previewPopup.hide()
    }

    private fun show() {
        previewLoadJob = coroutineScope.launch(Dispatchers.Default) {
            yield()
            val imageViewList = images.map {
                previewLoading(it)
            }.map {
                it.await()
            }
            yield()
            withContext(Dispatchers.JavaFx) {
                val hBox = HBox()
                imageViewList.forEach { hBox.add(it) }
                previewPopup.content.clear()
                previewPopup.content.add(hBox)
                previewPopup.show(owner)
            }
        }
    }

    private suspend fun previewLoading(url: String): Deferred<ImageView> {
        return coroutineScope.async(previewDispatcher) {
            val imageView = ImageView(Image(ByteArrayInputStream(PreviewCache.cache[url]))).apply {
                isPreserveRatio = true
                fitWidth = 200.0
            }
            val flow = imageView.image.progressProperty().asFlow()
            flow.filter { it == 1.0 }.map {
                imageView
            }.first()
        }
    }
}