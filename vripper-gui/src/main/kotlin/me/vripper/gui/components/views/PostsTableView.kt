package me.vripper.gui.components.views

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.MouseButton
import javafx.util.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import me.vripper.event.*
import me.vripper.event.EventBus
import me.vripper.gui.components.cells.PreviewTableCell
import me.vripper.gui.components.cells.ProgressTableCell
import me.vripper.gui.components.cells.StatusTableCell
import me.vripper.gui.components.fragments.AddLinksFragment
import me.vripper.gui.components.fragments.ColumnSelectionFragment
import me.vripper.gui.components.fragments.RenameFragment
import me.vripper.gui.controller.PostController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.model.PostModel
import me.vripper.gui.services.ClipboardService
import me.vripper.gui.utils.Preview
import me.vripper.gui.utils.openFileDirectory
import me.vripper.gui.utils.openLink
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class PostsTableView : View() {

    private val postController: PostController by inject()
    private val widgetsController: WidgetsController by inject()
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

        coroutineScope.launch {
            eventBus.events.filterIsInstance(PostCreateEvent::class).collect { postEvents ->
                val add = postEvents.posts.map { postController.mapper(it.postId) }
                runLater {
                    tableView.items.addAll(add)
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(PostUpdateEvent::class).collect { postEvents ->
                runLater {
                    for (post in postEvents.posts) {
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
                        postModel.path = post.getDownloadFolder()
                        postModel.folderName = post.folderName
                    }
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(PostDeleteEvent::class).collect { postEvents ->
                runLater {
                    postEvents.postIds.forEach {
                        items.removeIf { p -> p.postId == it }
                    }
                }
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(MetadataUpdateEvent::class).collect { metadataUpdateEvent ->
                runLater {
                    val postModel = items.find { it.postId == metadataUpdateEvent.metadata.postId } ?: return@runLater

                    postModel.altTitles =
                        FXCollections.observableArrayList(metadataUpdateEvent.metadata.data.resolvedNames)
                    postModel.postedby = metadataUpdateEvent.metadata.data.postedBy
                }
            }
        }
    }

    override val root = vbox {
        tableView = tableview(items) {
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setRowFactory {
                val tableRow = TableRow<PostModel>()

                val startItem = MenuItem("Start").apply {
                    setOnAction {
                        startSelected()
                    }
                    graphic = FontIcon.of(Feather.PLAY)
                }

                val stopItem = MenuItem("Stop").apply {
                    setOnAction {
                        stopSelected()
                    }
                    graphic = FontIcon.of(Feather.SQUARE)
                }

                val renameItem = MenuItem("Rename").apply {
                    setOnAction {
                        rename(tableRow.item)
                    }
                    graphic = FontIcon.of(Feather.EDIT)
                }

                val deleteItem = MenuItem("Delete").apply {
                    setOnAction {
                        deleteSelected()
                    }
                    graphic = FontIcon.of(Feather.TRASH)
                }

                val locationItem = MenuItem("Open containing folder").apply {
                    setOnAction {
                        openFileDirectory(tableRow.item.path)
                    }
                    graphic = FontIcon.of(Feather.FOLDER)
                }

                val urlItem = MenuItem("Open link").apply {
                    setOnAction {
                        openLink(tableRow.item.url)
                    }
                    graphic = FontIcon.of(Feather.LINK)
                }

                val contextMenu = ContextMenu()
                contextMenu.items.addAll(
                    startItem, stopItem, renameItem, deleteItem, SeparatorMenuItem(), locationItem, urlItem
                )
                tableRow.contextMenuProperty()
                    .bind(tableRow.emptyProperty().map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            contextMenu = ContextMenu()
            contextMenu.items.addAll(MenuItem("Add links").apply {
                graphic = FontIcon.of(Feather.PLUS)
                action {
                    find<AddLinksFragment>().apply {
                        input.clear()
                    }.openModal()
                }
            }, SeparatorMenuItem(), MenuItem("Setup columns").apply {
                graphic = FontIcon.of(Feather.COLUMNS)
                setOnAction {
                    find<ColumnSelectionFragment>(
                        mapOf(
                            ColumnSelectionFragment::map to mapOf(
                                Pair(
                                    "Preview", widgetsController.currentSettings.postsColumnsModel.previewProperty
                                ),
                                Pair(
                                    "Title", widgetsController.currentSettings.postsColumnsModel.titleProperty
                                ),
                                Pair(
                                    "Progress", widgetsController.currentSettings.postsColumnsModel.progressProperty
                                ),
                                Pair(
                                    "Status", widgetsController.currentSettings.postsColumnsModel.statusProperty
                                ),
                                Pair(
                                    "Path", widgetsController.currentSettings.postsColumnsModel.pathProperty
                                ),
                                Pair(
                                    "Total", widgetsController.currentSettings.postsColumnsModel.totalProperty
                                ),
                                Pair(
                                    "Hosts", widgetsController.currentSettings.postsColumnsModel.hostsProperty
                                ),
                                Pair(
                                    "Added On", widgetsController.currentSettings.postsColumnsModel.addedOnProperty
                                ),
                                Pair(
                                    "Order", widgetsController.currentSettings.postsColumnsModel.orderProperty
                                ),
                            )
                        )
                    ).openModal()
                }
            })
            column("Preview", PostModel::previewListProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.previewProperty)
                prefWidth = 100.0
                cellFactory = Callback {
                    val cell = PreviewTableCell<PostModel>()
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
                        if (cell.tableRow.item != null && cell.tableRow.item.previewList.isNotEmpty()) {
                            preview = Preview(currentStage!!, cell.tableRow.item.previewList)
                            preview?.previewPopup?.apply {
                                x = mouseEvent.screenX + 20
                                y = mouseEvent.screenY + 10
                            }
                        }
                    }
                    cell
                }
            }
            column("Title", PostModel::titleProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.titleProperty)
                prefWidth = 250.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Progress", PostModel::progressProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.progressProperty)
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
                                }
                            }

                            else -> {}
                        }
                    }
                    cell as TableCell<PostModel, Number>
                }
            }
            column("Status", PostModel::statusProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.statusProperty)
                prefWidth = 100.0
                cellFactory = Callback {
                    StatusTableCell<PostModel>()
                }
            }
            column("Path", PostModel::pathProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.pathProperty)
                prefWidth = 250.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Total", PostModel::progressCountProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.totalProperty)
                prefWidth = 150.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Hosts", PostModel::hostsProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.hostsProperty)
                prefWidth = 100.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Added On", PostModel::addedOnProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.addedOnProperty)
                prefWidth = 125.0
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
            column("Order", PostModel::orderProperty) {
                visibleProperty().bind(widgetsController.currentSettings.postsColumnsModel.orderProperty)
                prefWidth = 100.0
                sortOrder.add(this)
                cellFactory = Callback {
                    TextFieldTableCell<PostModel?, Number?>().apply { alignment = Pos.CENTER_LEFT }
                }
            }
        }
    }

    private fun rename(post: PostModel) {
        find<RenameFragment>(
            mapOf(
                RenameFragment::postId to post.postId,
                RenameFragment::name to post.folderName,
                RenameFragment::altTitles to post.altTitles
            )
        ).openModal()?.apply {
            minWidth = 450.0
        }
    }

    fun renameSelected() {
        val selectedItem = tableView.selectionModel.selectedItem
        if (selectedItem != null) {
            rename(selectedItem)
        }
    }

    fun deleteSelected() {
        val postIdList = tableView.selectionModel.selectedItems.map { it.postId }
        confirm(
            "",
            "Confirm removal of ${postIdList.size} post${if (postIdList.size > 1) "s" else ""}?",
            ButtonType.YES,
            ButtonType.NO,
            owner = primaryStage,
            title = "Remove posts"
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
}