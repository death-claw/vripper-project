package me.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.util.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.event.EventBus
import me.vripper.event.PostEvent
import me.vripper.gui.clipboard.ClipboardService
import me.vripper.gui.controller.PostController
import me.vripper.gui.model.PostModel
import me.vripper.gui.view.*
import tornadofx.*
import java.time.Duration

class PostsTableView : View() {

    private val postController: PostController by inject()
    private val clipboardService: ClipboardService by di()
    private val eventBus: EventBus by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var tableView: TableView<PostModel>
    private var items: ObservableList<PostModel> = FXCollections.observableArrayList()
    private var preview: Preview? = null

    init {
        titleProperty.bind(items.sizeProperty.map {
            if (it.toLong() > 0) {
                "Download (${it.toLong()})"
            } else {
                "Download"
            }
        })
    }

    override fun onDock() {
        tableView.prefHeightProperty().bind(root.heightProperty())
        tableView.placeholder = Label("Loading")
        coroutineScope.launch {
            val postModelList = postController.findAllPosts().await()
            runLater {
                items.addAll(postModelList)
                tableView.placeholder = Label("No content in table")
                clipboardService.init()
            }
        }
        eventBus.events.ofType(PostEvent::class.java).buffer(Duration.ofMillis(125)).subscribe { postEvents ->
            runLater {
                postEvents.map { it.delete }.flatten().forEach {
                    items.removeIf { p -> p.postId == it }
                }
                val add = postEvents.map { it.add }.flatten().map { postController.mapper(it) }
                tableView.items.addAll(add)

                for (post in postEvents.map { it.update }.flatten().reversed().distinct()) {
                    val postModel = items.find { it.postId == post.postId } ?: continue

                    postModel.status = post.status.name
                    postModel.progressCount = postController.progressCount(
                        post.total, post.done, post.downloaded
                    )
                    postModel.order = post.rank + 1
                    postModel.done = post.done
                    postModel.progress = postController.progress(
                        post.total, post.done
                    )
                }
            }
        }
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
            column("Preview", PostModel::previewListProperty) {
                prefWidth = 50.0
                cellFactory = Callback {
                    val cell = PreviewTableCell<PostModel>()
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
            column("Title", PostModel::titleProperty) {
                prefWidth = 250.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Progress", PostModel::progressProperty) {
                prefWidth = 100.0
                cellFactory = Callback {
                    val cell = ProgressTableCell<PostModel>()
                    cell.alignment = Pos.CENTER
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
            column("Status", PostModel::statusProperty) {
                prefWidth = 50.0
                cellFactory = Callback {
                    StatusTableCell<PostModel>()
                }
            }
            column("Path", PostModel::pathProperty) {
                prefWidth = 250.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Total", PostModel::progressCountProperty) {
                prefWidth = 150.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Hosts", PostModel::hostsProperty) {
                prefWidth = 100.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Added On", PostModel::addedOnProperty) {
                prefWidth = 100.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Order", PostModel::orderProperty) {
                prefWidth = 50.0
                sortOrder.add(this)
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, Number?>().apply { alignment = Pos.CENTER_LEFT }
                }
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

    private fun openPhotos(postId: Long) {
        find<ImagesTableView>(mapOf(ImagesTableView::postId to postId)).openModal()?.apply {
            minWidth = 600.0
            minHeight = 400.0
        }
    }
}