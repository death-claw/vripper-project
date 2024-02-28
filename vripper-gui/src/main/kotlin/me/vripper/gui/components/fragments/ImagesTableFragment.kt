package me.vripper.gui.components.fragments

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.MouseButton
import javafx.util.Callback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import me.vripper.entities.Image
import me.vripper.entities.domain.Status
import me.vripper.event.EventBus
import me.vripper.event.ImageEvent
import me.vripper.event.StoppedEvent
import me.vripper.gui.components.cells.PreviewTableCell
import me.vripper.gui.components.cells.ProgressTableCell
import me.vripper.gui.components.cells.StatusTableCell
import me.vripper.gui.controller.ImageController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.model.ImageModel
import me.vripper.gui.utils.Preview
import me.vripper.gui.utils.openLink
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class ImagesTableFragment : Fragment("Photos") {

    private lateinit var tableView: TableView<ImageModel>
    private val imageController: ImageController by inject()
    private val widgetsController: WidgetsController by inject()
    private val eventBus: EventBus by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val items: ObservableList<ImageModel> = FXCollections.observableArrayList()
    private var preview: Preview? = null

    fun setPostId(postId: Long) {
        coroutineScope.launch {
            val list = coroutineScope.async {
                imageController.findImages(postId)
            }.await()
            runLater {
                items.addAll(list)
                tableView.sort()
                tableView.placeholder = Label("No content in table")
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(ImageEvent::class).map {
                it.copy(images = it.images.filter { image: Image -> image.postId == postId })
            }.filter {
                it.images.isNotEmpty()
            }.collect { imageEvent ->
                runLater {
                    for (image in imageEvent.images) {
                        val imageModel = items.find { it.id == image.id } ?: continue

                        imageModel.size = image.size
                        imageModel.status = image.status.name
                        imageModel.filename = image.filename
                        imageModel.downloaded = image.downloaded
                        imageModel.progress = imageController.progress(
                            image.size, image.downloaded
                        )
                    }
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(StoppedEvent::class).collect { stoppedEvent ->
                runLater {
                    items.forEach { imageModel ->
                        if (imageModel.status != Status.FINISHED.name) {
                            imageModel.status = Status.STOPPED.name
                        }
                    }
                }
            }
        }
    }

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 550.0
    }

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        tableView = tableview(items) {
            setRowFactory {
                val tableRow = TableRow<ImageModel>()
                val urlItem = MenuItem("Open link").apply {
                    setOnAction {
                        openLink(tableRow.item.url)
                    }
                    graphic = FontIcon.of(Feather.LINK)
                }
                val contextMenu = ContextMenu()
                contextMenu.items.addAll(urlItem)
                tableRow.contextMenuProperty().bind(tableRow.emptyProperty()
                    .map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            contextMenu = ContextMenu()
            contextMenu.items.add(MenuItem("Setup columns").apply {
                setOnAction {
                    find<ColumnSelectionFragment>(
                        mapOf(
                            ColumnSelectionFragment::map to mapOf(
                                Pair(
                                    "Preview",
                                    widgetsController.currentSettings.imagesColumnsModel.previewProperty
                                ),
                                Pair(
                                    "Index",
                                    widgetsController.currentSettings.imagesColumnsModel.indexProperty
                                ),
                                Pair(
                                    "Link",
                                    widgetsController.currentSettings.imagesColumnsModel.linkProperty
                                ),
                                Pair(
                                    "Progress",
                                    widgetsController.currentSettings.imagesColumnsModel.progressProperty
                                ),
                                Pair(
                                    "Filename",
                                    widgetsController.currentSettings.imagesColumnsModel.filenameProperty
                                ),
                                Pair(
                                    "Status",
                                    widgetsController.currentSettings.imagesColumnsModel.statusProperty
                                ),
                                Pair(
                                    "Size",
                                    widgetsController.currentSettings.imagesColumnsModel.sizeProperty
                                ),
                                Pair(
                                    "Downloaded",
                                    widgetsController.currentSettings.imagesColumnsModel.downloadedProperty
                                ),
                            )
                        )
                    ).openModal()
                }
                graphic = FontIcon.of(Feather.COLUMNS)
            })
            column("Preview", ImageModel::thumbUrlProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.previewProperty)
                prefWidth = 100.0
                cellFactory = Callback {
                    val cell = PreviewTableCell<ImageModel>()
                    cell.onMouseExited = EventHandler {
                        preview?.hide()
                    }
                    cell.onMouseMoved = EventHandler {
                        preview?.previewPopup?.apply {
                            x = it.screenX + 20
                            y = it.screenY + 10
                        }
                    }
                    cell.onMouseEntered = EventHandler { mouseEvent ->
                        preview?.hide()
                        if (cell.tableRow.item != null && cell.tableRow.item.thumbUrl.isNotEmpty()) {
                            preview = Preview(currentStage!!, cell.tableRow.item.thumbUrl)
                            preview?.previewPopup?.apply {
                                x = mouseEvent.screenX + 20
                                y = mouseEvent.screenY + 10
                            }
                        }
                    }
                    cell
                }
            }
            column("Index", ImageModel::indexProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.indexProperty)
                prefWidth = 100.0
                sortOrder.add(this)
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, Number?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Link", ImageModel::urlProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.linkProperty)
                prefWidth = 200.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Progress", ImageModel::progressProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.progressProperty)
                prefWidth = 100.0
                cellFactory = Callback {
                    val cell = ProgressTableCell<ImageModel>()
                    cell.alignment = Pos.CENTER
                    cell.setOnMouseClick {
                        when (it.clickCount) {
                            1 -> {
                                this@tableview.requestFocus()
                                this@tableview.focusModel.focus(cell.tableRow.index)
                                if (it.button.equals(MouseButton.PRIMARY)) {
                                    this@tableview.selectionModel.clearSelection()
                                    this@tableview.selectionModel.select(cell.tableRow.index)
                                }
                            }
                        }
                    }
                    cell as TableCell<ImageModel, Number>
                }
            }
            column("Filename", ImageModel::filenameProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.filenameProperty)
                prefWidth = 150.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Status", ImageModel::statusProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.statusProperty)
                prefWidth = 100.0
                cellFactory = Callback {
                    StatusTableCell()
                }
            }
            column("Size", ImageModel::sizeProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.sizeProperty)
                prefWidth = 75.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Downloaded", ImageModel::downloadedProperty) {
                visibleProperty().bind(widgetsController.currentSettings.imagesColumnsModel.downloadedProperty)
                prefWidth = 125.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
        }
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}