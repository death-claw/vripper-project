package me.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.util.Callback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import me.vripper.entities.Image
import me.vripper.event.EventBus
import me.vripper.event.ImageEvent
import me.vripper.gui.controller.ImageController
import me.vripper.gui.model.ImageModel
import me.vripper.gui.view.ProgressTableCell
import me.vripper.gui.view.StatusTableCell
import me.vripper.gui.view.openLink
import tornadofx.*

class ImagesTableView : Fragment("Photos") {

    private lateinit var tableView: TableView<ImageModel>
    private val imageController: ImageController by inject()
    private val eventBus: EventBus by di()
    private var items: ObservableList<ImageModel> = FXCollections.observableArrayList()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val postId: Long by param()

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 550.0
        tableView.placeholder = Label("Loading")
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
    }

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        tableView = tableview(items) {
            setRowFactory {
                val tableRow = TableRow<ImageModel>()
                val urlItem = MenuItem("Open link").apply {
                    setOnAction {
                        openLink(tableRow.item.url)
                    }
                    graphic = ImageView("open-in-browser.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                }
                val contextMenu = ContextMenu()
                contextMenu.items.addAll(urlItem)
                tableRow.contextMenuProperty().bind(tableRow.emptyProperty()
                    .map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            column("Index", ImageModel::indexProperty) {
                prefWidth = 50.0
                sortOrder.add(this)
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, Number?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Link", ImageModel::urlProperty) {
                prefWidth = 200.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Progress", ImageModel::progressProperty) {
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
                prefWidth = 150.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Status", ImageModel::statusProperty) {
                prefWidth = 50.0
                cellFactory = Callback {
                    StatusTableCell()
                }
            }
            column("Size", ImageModel::sizeProperty) {
                prefWidth = 75.0
                cellFactory = Callback {
                    TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Downloaded", ImageModel::downloadedProperty) {
                prefWidth = 75.0
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