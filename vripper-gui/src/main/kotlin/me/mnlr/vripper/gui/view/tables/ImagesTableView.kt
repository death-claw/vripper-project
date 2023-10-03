package me.mnlr.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.util.Callback
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.gui.controller.ImageController
import me.mnlr.vripper.gui.model.ImageModel
import me.mnlr.vripper.gui.view.FxScheduler
import me.mnlr.vripper.gui.view.ProgressTableCell
import me.mnlr.vripper.gui.view.openLink
import tornadofx.*

class ImagesTableView : Fragment("Photos") {

    private lateinit var tableView: TableView<ImageModel>
    private val imageController: ImageController by inject()
    private val eventBus: EventBus by di()
    private var items: ObservableList<ImageModel> = FXCollections.observableArrayList()
    val postId: String by param()

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 600.0

        runLater {
            items.addAll(imageController.findImages(postId))
            tableView.sort()
        }

        val disposable = eventBus
            .flux()
            .filter { it!!.kind == Event.Kind.IMAGE_UPDATE }
            .map { imageController.findImageById(it!!.data as Long) }
            .filter { it.isPresent }
            .map { it.get() }
            .filter { it.postId == postId }
            .publishOn(FxScheduler)
            .doOnNext { image ->
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
            }.subscribe()

        whenUndocked {
            disposable.dispose()
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
            }
            column("Link", ImageModel::urlProperty) {
                prefWidth = 200.0
            }
            column("Progress", ImageModel::progressProperty) {
                cellFactory = Callback {
                    val cell = ProgressTableCell<ImageModel>()
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
            column("Status", ImageModel::statusProperty)
        }
    }
}