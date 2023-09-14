package me.mnlr.vripper.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.util.Callback
import me.mnlr.vripper.controller.PostController
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.model.PostModel
import me.mnlr.vripper.view.ProgressTableCell
import me.mnlr.vripper.view.openFileDirectory
import me.mnlr.vripper.view.openLink
import tornadofx.*


class PostsTableView : View() {

    private val postController: PostController by inject()
    private val eventBus: EventBus by di()

    lateinit var tableView: TableView<PostModel>
    private var items: ObservableList<PostModel> = FXCollections.observableArrayList()

    init {
        titleProperty.bind(items.sizeProperty.map {
            if (it.toLong() > 0) {
                "Download (${it.toLong()})"
            } else {
                "Download"
            }
        })
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
                        progressCount = it.progressCount
                    }
                } else {
                    items.add(it)
                    runLater {
                        this.tableView.refresh()
                    }
                }
            }

        }

        eventBus.flux().filter {
            it!!.kind == Event.Kind.POST_REMOVE
        }.subscribe { event ->
            items.removeIf { p -> p.postId == event.data as String }
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

                val startItem = MenuItem("Start").apply {
                    setOnAction {
                        startSelected()
                    }
                    graphic = ImageView("play.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                }

                val stopItem = MenuItem("Stop").apply {
                    setOnAction {
                        stopSelected()
                    }
                    graphic = ImageView("pause.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                }

                val deleteItem = MenuItem("Delete").apply {
                    setOnAction {
                        deleteSelected()
                    }
                    graphic = ImageView("trash.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                }

                val detailsItem = MenuItem("Images").apply {
                    setOnAction {
                        openPhotos(tableRow.item.postId)
                    }
                    graphic = ImageView("details.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                }

                val locationItem = MenuItem("Open containing folder").apply {
                    setOnAction {
                        openFileDirectory(tableRow.item.path)
                    }
                    graphic = ImageView("file-explorer.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
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
                contextMenu.items.addAll(
                    startItem, stopItem, deleteItem, SeparatorMenuItem(), detailsItem, locationItem, urlItem
                )
                tableRow.contextMenuProperty()
                    .bind(tableRow.emptyProperty().map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            column("Title", PostModel::titleProperty) {
                prefWidth = 300.0
            }
            column("Progress", PostModel::progressProperty) {
                cellFactory = Callback {
                    val cell = ProgressTableCell<PostModel>()
                    cell.setOnMouseClick {
                        when (it.button) {
                            MouseButton.SECONDARY -> {
                                this@tableview.requestFocus()
                                this@tableview.focusModel.focus(cell.tableRow.index)
                                if (!this@tableview.selectionModel.isSelected(cell.tableRow.index)) {
                                    this@tableview.selectionModel.clearSelection()
                                    this@tableview.selectionModel.select(cell.tableRow.index)
                                }
                            }
                            MouseButton.PRIMARY -> {
                                when (it.clickCount) {
                                    1 -> {
                                        this@tableview.requestFocus()
                                        this@tableview.focusModel.focus(cell.tableRow.index)
                                        if (it.isControlDown && it.button.equals(MouseButton.PRIMARY)) {
                                            if (this@tableview.selectionModel.isSelected(cell.tableRow.index)) {
                                                this@tableview.selectionModel.clearSelection(cell.tableRow.index)
                                            } else {
                                                this@tableview.selectionModel.select(cell.tableRow.index)
                                            }
                                        } else if (it.button.equals(MouseButton.PRIMARY)) {
                                            this@tableview.selectionModel.clearSelection()
                                            this@tableview.selectionModel.select(cell.tableRow.index)
                                        }
                                    }
                                    2 -> openPhotos(cell.tableRow.item.postId)
                                }
                            }
                            else -> {}
                        }
                    }
                    cell as TableCell<PostModel, Number>
                }
            }
            column("Status", PostModel::statusProperty)
            column("Path", PostModel::pathProperty)
            column("Total", PostModel::progressCountProperty)
            column("Hosts", PostModel::hostsProperty)
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