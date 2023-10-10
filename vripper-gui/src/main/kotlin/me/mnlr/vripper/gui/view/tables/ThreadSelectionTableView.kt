package me.mnlr.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.util.Callback
import me.mnlr.vripper.gui.controller.ThreadController
import me.mnlr.vripper.gui.model.ThreadSelectionModel
import me.mnlr.vripper.gui.view.Preview
import me.mnlr.vripper.gui.view.PreviewTableCell
import me.mnlr.vripper.gui.view.openLink
import tornadofx.*

class ThreadSelectionTableView : Fragment("Thread") {

    private lateinit var tableView: TableView<ThreadSelectionModel>
    private val threadController: ThreadController by inject()
    private var items: ObservableList<ThreadSelectionModel> = FXCollections.observableArrayList()
    private var preview: Preview? = null
    val threadId: String by param()

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        tableView.placeholder = Label("Loading")
        runLater {
            items.addAll(threadController.grab(threadId))
        }
    }

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        spacing = 5.0
        tableView = tableview(items) {
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setRowFactory {
                val tableRow = TableRow<ThreadSelectionModel>()

                tableRow.setOnMouseClicked {
                    if (it.button.equals(MouseButton.PRIMARY) && it.clickCount == 2 && tableRow.item != null) {
                        threadController.download(listOf(tableRow.item))
                        close()
                    }
                }

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
            column("Preview", ThreadSelectionModel::previewListProperty) {
                cellFactory = Callback {
                    val cell = PreviewTableCell<ThreadSelectionModel>()
                    cell.alignment = Pos.CENTER
                    cell.onMouseEntered = EventHandler { mouseEvent ->
                        if (cell.tableRow.item != null && cell.tableRow.item.previewList.isNotEmpty()) {
                            preview = Preview(currentStage!!, cell.tableRow.item.previewList)
                            preview?.previewPopup?.apply {
                                x = mouseEvent.screenX + 20
                                y = mouseEvent.screenY + 10
                            }
                        }
                    }
                    cell.onMouseMoved = EventHandler {
                        preview?.previewPopup?.apply {
                            x = it.screenX + 20
                            y = it.screenY + 10
                        }
                    }
                    cell.onMouseExited = EventHandler {
                        preview?.hide()
                    }
                    cell
                }
            }
            column("Post Index", ThreadSelectionModel::indexProperty) {
                sortOrder.add(this)
                cellFactory = Callback {
                    TextFieldTableCell<ThreadSelectionModel?, Number?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Title", ThreadSelectionModel::titleProperty) {
                prefWidth = 200.0
                cellFactory = Callback {
                    TextFieldTableCell<ThreadSelectionModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("URL", ThreadSelectionModel::urlProperty) {
                prefWidth = 200.0
                cellFactory = Callback {
                    TextFieldTableCell<ThreadSelectionModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Hosts", ThreadSelectionModel::hostsProperty) {
                cellFactory = Callback {
                    TextFieldTableCell<ThreadSelectionModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
        }
        borderpane {
            right {
                padding = insets(top = 0, right = 5, bottom = 5, left = 5)
                button("Download") {
                    imageview("download.png") {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                    tooltip("Download selected posts")
                    enableWhen { tableView.selectionModel.selectedItems.sizeProperty.greaterThan(0) }
                    action {
                        threadController.download(tableView.selectionModel.selectedItems)
                        close()
                    }
                }
            }
        }
    }
}