package me.mnlr.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.util.Callback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.javafx.JavaFx
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.ImageUpdateEvent
import me.mnlr.vripper.gui.controller.ImageController
import me.mnlr.vripper.gui.model.ImageModel
import me.mnlr.vripper.gui.view.ProgressTableCell
import me.mnlr.vripper.gui.view.StatusTableCell
import me.mnlr.vripper.gui.view.openLink
import tornadofx.*

class ImagesTableView : Fragment("Photos") {

    private lateinit var tableView: TableView<ImageModel>
    private val imageController: ImageController by inject()
    private val eventBus: EventBus by di()
    private var items: ObservableList<ImageModel> = FXCollections.observableArrayList()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val postId: String by param()

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 600.0

        runLater {
            items.addAll(imageController.findImages(postId))
            tableView.sort()
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance<ImageUpdateEvent>().filter {
                it.image.postId == postId
            }.collectLatest { imageEvent ->
                coroutineContext.ensureActive()
                val image = imageController.mapper(imageEvent.image)
                withContext(Dispatchers.JavaFx) {
                    val find = items
                        .find { it.id == image.id }
                    if (find != null) {
                        find.apply {
                            progress = image.progress
                            status = image.status
                        }
                    } else {
                        items.add(image)
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
            column("Status", ImageModel::statusProperty) {
                cellFactory = Callback {
                    StatusTableCell()
                }
            }
        }
    }
}