package me.vripper.gui.components.views

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.image.ImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import me.vripper.event.EventBus
import me.vripper.event.ThreadClearEvent
import me.vripper.event.ThreadCreateEvent
import me.vripper.event.ThreadDeleteEvent
import me.vripper.gui.components.fragments.ColumnSelectionFragment
import me.vripper.gui.components.fragments.ThreadSelectionTableFragment
import me.vripper.gui.controller.ThreadController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.model.ThreadModel
import me.vripper.gui.utils.openLink
import tornadofx.*

class ThreadTableView : View() {

    private val threadController: ThreadController by inject()
    private val widgetsController: WidgetsController by inject()
    private val eventBus: EventBus by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var tableView: TableView<ThreadModel>
    private var items: ObservableList<ThreadModel> = FXCollections.observableArrayList()

    init {
        titleProperty.bind(items.sizeProperty.map {
            if (it.toLong() > 0) {
                "Threads (${it.toLong()})"
            } else {
                "Threads"
            }
        })
    }

    override fun onDock() {
        tableView.prefHeightProperty().bind(root.heightProperty())
        tableView.placeholder = Label("Loading")
        coroutineScope.launch {
            val list = coroutineScope.async {
                threadController.findAll()
            }.await()
            runLater {
                items.addAll(list)
                tableView.placeholder = Label("No content in table")
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(ThreadCreateEvent::class).collect {
                val threadModelMapper = threadController.threadModelMapper(it.thread)
                runLater {
                    items.add(threadModelMapper)
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(ThreadCreateEvent::class).collect {
                val threadModelMapper = threadController.threadModelMapper(it.thread)
                runLater {
                    items.add(threadModelMapper)
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(ThreadDeleteEvent::class).collect { event ->
                runLater {
                    tableView.items.removeIf { it.threadId == event.threadId }
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(ThreadClearEvent::class).collect {
                runLater {
                    tableView.items.clear()
                }
            }
        }
    }

    override val root = vbox {
        tableView = tableview(items) {
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setRowFactory {
                val tableRow = TableRow<ThreadModel>()

                tableRow.setOnMouseClicked {
                    if (it.clickCount == 2 && tableRow.item != null) {
                        selectPosts(tableRow.item.threadId)
                    }
                }

                val selectItem = MenuItem("Select posts").apply {
                    setOnAction {
                        selectPosts(tableRow.item.threadId)
                    }
                    graphic = ImageView("popup.png").apply {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                }

                val urlItem = MenuItem("Open link").apply {
                    setOnAction {
                        openLink(tableRow.item.link)
                    }
                    graphic = ImageView("open-in-browser.png").apply {
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

                val contextMenu = ContextMenu()
                contextMenu.items.addAll(selectItem, urlItem, SeparatorMenuItem(), deleteItem)
                tableRow.contextMenuProperty()
                    .bind(tableRow.emptyProperty().map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            contextMenu = ContextMenu()
            contextMenu.items.add(MenuItem("Setup columns").apply {
                setOnAction {
                    find<ColumnSelectionFragment>(
                        mapOf(
                            ColumnSelectionFragment::map to mapOf(
                                Pair(
                                    "Title", widgetsController.currentSettings.threadsColumnsModel.titleProperty
                                ),
                                Pair(
                                    "URL", widgetsController.currentSettings.threadsColumnsModel.linkProperty
                                ),
                                Pair(
                                    "Count", widgetsController.currentSettings.threadsColumnsModel.countProperty
                                ),
                            )
                        )
                    ).openModal()
                }
                graphic = ImageView("columns.png").apply {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
            })
            column("Title", ThreadModel::titleProperty) {
                visibleProperty().bind(widgetsController.currentSettings.threadsColumnsModel.titleProperty)
                prefWidth = 350.0
            }
            column("URL", ThreadModel::linkProperty) {
                visibleProperty().bind(widgetsController.currentSettings.threadsColumnsModel.linkProperty)
                prefWidth = 350.0
            }
            column("Count", ThreadModel::totalProperty) {
                visibleProperty().bind(widgetsController.currentSettings.threadsColumnsModel.countProperty)
            }
        }
    }

    fun deleteSelected() {
        val threadIdList = tableView.selectionModel.selectedItems.map { it.threadId }
        confirm(
            "Clean threads",
            "Confirm removal of ${threadIdList.size} thread${if (threadIdList.size > 1) "s" else ""}",
            ButtonType.YES,
            ButtonType.NO
        ) {
            threadController.delete(threadIdList)
        }
    }

    private fun selectPosts(threadId: Long) {
        find<ThreadSelectionTableFragment>(mapOf(ThreadSelectionTableFragment::threadId to threadId)).openModal()
            ?.apply {
                minWidth = 600.0
                minHeight = 400.0
                width = 800.0
                height = 600.0
            }
    }
}