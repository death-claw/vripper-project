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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.mnlr.vripper.entities.Post
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.gui.controller.PostController
import me.mnlr.vripper.gui.model.PostModel
import me.mnlr.vripper.gui.view.*
import me.mnlr.vripper.gui.view.PreviewCache.previewDispatcher
import tornadofx.*


class PostsTableView : View() {

    private val postController: PostController by inject()
    private val eventBus: EventBus by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

        eventBus.flux().filter { it!!.kind == Event.Kind.POST_CREATE }
            .map { postController.mapper(it.data as Post) }.publishOn(FxScheduler)
            .doOnNext { postModel ->
                items.add(postModel)
                this.tableView.refresh()
                postModel.previewList.forEach {
                    coroutineScope.launch(previewDispatcher) {
                        PreviewCache.cache.asMap()[it] = PreviewCache.load(it)
                    }
                }
            }.subscribe()

        eventBus.flux().filter { it!!.kind == Event.Kind.POST_UPDATE }
            .map { postController.mapper(it.data as Post) }.publishOn(FxScheduler)
            .doOnNext {
                items.find { postModel -> postModel.postId == it.postId }?.apply {
                    progress = it.progress
                    status = it.status
                    done = it.done
                    total = it.total
                    order = it.order
                    progressCount = it.progressCount
                }
            }.subscribe()

        eventBus.flux().filter { it!!.kind == Event.Kind.POST_REMOVE }.publishOn(FxScheduler)
            .doOnNext {
                items.removeIf { p -> p.postId == it.data as String }
            }.subscribe()
    }

    override fun onDock() {
        tableView.prefHeightProperty().bind(root.heightProperty())
        coroutineScope.launch {
            val postModelList = postController.findAllPosts().await()
            items.addAll(postModelList)
            postModelList.flatMap { it.previewList }.forEach {
                launch(previewDispatcher) {
                    PreviewCache.cache.asMap()[it] = PreviewCache.load(it)
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
                    startItem,
                    stopItem,
                    deleteItem,
                    SeparatorMenuItem(),
                    detailsItem,
                    locationItem,
                    urlItem
                )
                tableRow.contextMenuProperty().bind(tableRow.emptyProperty()
                    .map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            column("Preview", PostModel::previewListProperty) {
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
                prefWidth = 300.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Progress", PostModel::progressProperty) {
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
                cellFactory = Callback {
                    StatusTableCell<PostModel>()
                }
            }
            column("Path", PostModel::pathProperty) {
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Total", PostModel::progressCountProperty) {
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Hosts", PostModel::hostsProperty) {
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Added On", PostModel::addedOnProperty) {
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Order", PostModel::orderProperty) {
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

    private fun openPhotos(postId: String) {
        find<ImagesTableView>(mapOf(ImagesTableView::postId to postId)).openModal()?.apply {
            minWidth = 600.0
            minHeight = 400.0
        }
    }
}