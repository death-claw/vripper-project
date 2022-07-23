package me.mnlr.vripper.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import me.mnlr.vripper.controller.PostController
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.model.PostModel
import tornadofx.*


class PostsTableView : View("Download") {

    private val postController: PostController by inject()
    private val eventBus: EventBus by di()

    lateinit var tableView: TableView<PostModel>
    private var items: ObservableList<PostModel> = FXCollections.observableArrayList()

    init {
        items.addAll(postController.findAllPosts())

        eventBus.flux().filter {
            it!!.kind == Event.Kind.POST_UPDATE || it.kind == Event.Kind.METADATA_UPDATE
        }.subscribe { event ->
            postController.findById(event!!.data as Long).ifPresent {
                // search
                val find = items.find { postModel -> postModel.postId == it.postId }
                if (find != null) {
                    find.apply {
                        progress = it.progress
                        status = it.status
                        done = it.done
                        total = it.total
                        order = it.order
                    }
                } else {
                    items.add(it)
                    runLater {
                        this.tableView.refresh()
                    }
                }
            }

        }
    }

    override fun onDock() {
        tableView.prefHeightProperty().bind(root.heightProperty())
    }

    override val root = vbox {
        tableView = tableview(items) {
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setRowFactory {
                val tableRow = TableRow<PostModel>()
                tableRow.setOnMouseClicked {
                    if (it.clickCount == 2 && tableRow.item != null) {
                        openPhotos(tableRow.item.postId)
                    }
                }

                val contextMenu = ContextMenu()
                val startItem = MenuItem("Start")
                startItem.setOnAction {
                    startSelected()
                }
                val playIcon = ImageView("play.png")
                playIcon.fitWidth = 18.0
                playIcon.fitHeight = 18.0
                startItem.graphic = playIcon

                val stopItem = MenuItem("Stop")
                stopItem.setOnAction {
                    stopSelected()
                }
                val stopIcon = ImageView("pause.png")
                stopIcon.fitWidth = 18.0
                stopIcon.fitHeight = 18.0
                stopItem.graphic = stopIcon

                val deleteItem = MenuItem("Delete")
                deleteItem.setOnAction {
                    deleteSelected()
                }
                val deleteIcon = ImageView("trash.png")
                deleteIcon.fitWidth = 18.0
                deleteIcon.fitHeight = 18.0
                deleteItem.graphic = deleteIcon

                val detailsItem = MenuItem("Images")
                detailsItem.setOnAction {
                    openPhotos(tableRow.item.postId)
                }
                val detailsIcon = ImageView("details.png")
                detailsIcon.fitWidth = 18.0
                detailsIcon.fitHeight = 18.0
                detailsItem.graphic = detailsIcon

                contextMenu.items.addAll(
                    startItem, stopItem, deleteItem, SeparatorMenuItem(), detailsItem
                )
                tableRow.contextMenuProperty()
                    .bind(tableRow.emptyProperty().map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            column("Title", PostModel::titleProperty) {
                prefWidth = 300.0
            }
            column("Progress", PostModel::progressProperty) {
                cellFormat {
                    addClass(Stylesheet.progressBarTableCell)
                    graphic = cache {
                        progressbar(itemProperty().doubleBinding { it?.toDouble() ?: 0.0 }) {
                            setOnMouseClicked {
                                when (it.clickCount) {
                                    1 -> {
                                        this@tableview.requestFocus()
                                        this@tableview.focusModel.focus(this@cellFormat.tableRow.index)
                                        if (it.isControlDown && it.button.equals(MouseButton.PRIMARY)) {
                                            if (this@tableview.selectionModel.isSelected(this@cellFormat.tableRow.index)) {
                                                this@tableview.selectionModel.clearSelection(this@cellFormat.tableRow.index)
                                            } else {
                                                this@tableview.selectionModel.select(this@cellFormat.tableRow.index)
                                            }
                                        } else if (it.button.equals(MouseButton.PRIMARY)) {
                                            this@tableview.selectionModel.clearSelection()
                                            this@tableview.selectionModel.select(this@cellFormat.tableRow.index)
                                        }
                                    }
                                    2 -> openPhotos(this@cellFormat.tableRow.item.postId)
                                }
                            }
                            useMaxWidth = true
                        }
                    }
                }
            }
            column("Status", PostModel::statusProperty)
            column("Path", PostModel::pathProperty)
            column("Images", PostModel::totalProperty)
            column("Added On", PostModel::addedOnProperty)
            column("Order", PostModel::orderProperty) {
                sortOrder.add(this)
            }
        }
    }

    fun deleteSelected() {
        val postIdList = tableView.selectionModel.selectedItems.map { it.postId }
        confirm(
            "Remove posts",
            "Confirm removal of ${postIdList.size} post${if (postIdList.size > 1) "s" else ""}",
            ButtonType.YES,
            ButtonType.NO
        ) {
            postController.delete(postIdList)
            tableView.items.removeIf { postIdList.contains(it.postId) }
        }
    }

    fun stopSelected() {
        val postIdList = tableView.selectionModel.selectedItems.map { it.postId }
        postController.stop(postIdList)
    }

    fun startSelected() {
        val postIdList = tableView.selectionModel.selectedItems.map { it.postId }
        postController.start(postIdList)
    }

    private fun openPhotos(postId: String) {
        find<ImagesTableView>(mapOf(ImagesTableView::postId to postId)).openModal()
    }
}