package me.mnlr.vripper.gui.view

import javafx.scene.control.ProgressIndicator
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.javafx.asFlow
import tornadofx.*
import java.io.ByteArrayInputStream

class Preview : Fragment("Preview") {

    val images: List<String> by param()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var previewLoadJob: Job? = null

    override val root = anchorpane()

    override fun onDock() {
        currentStage?.width = 52.0
        currentStage?.height = 52.0
        root.add(ProgressIndicator().apply { minWidth = 50.0; minHeight = 50.0 })
        show()
    }

    override fun onUndock() {
        previewLoadJob?.cancel()
        coroutineScope.cancel()
    }

    private fun show() {
        previewLoadJob = coroutineScope.launch(Dispatchers.Default) {
            yield()
            val imageViewList = images.map {
                previewLoading(it).await()
            }
            yield()
            withContext(Dispatchers.JavaFx) {
                val hBox = HBox()
                imageViewList.forEach { hBox.add(it) }
                root.clear()
                root.add(hBox)
                val maxHeight = imageViewList.maxOf { it.boundsInParent.height }
                this@Preview.currentStage?.width = 200.0 * 4
                this@Preview.currentStage?.height = maxHeight
            }
        }
    }

    private suspend fun previewLoading(url: String): Deferred<ImageView> {
        return coroutineScope.async(Dispatchers.IO) {
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