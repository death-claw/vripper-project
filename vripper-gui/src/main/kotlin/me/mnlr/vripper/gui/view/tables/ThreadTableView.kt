package me.mnlr.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.image.ImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.event.ThreadClearEvent
import me.mnlr.vripper.event.ThreadCreateEvent
import me.mnlr.vripper.event.ThreadDeleteEvent
import me.mnlr.vripper.gui.controller.ThreadController
import me.mnlr.vripper.gui.model.ThreadModel
import me.mnlr.vripper.gui.view.openLink
import tornadofx.*

class ThreadTableView : View() {

    private val threadController: ThreadController by inject()
    private val eventBus: EventBus by di()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        items.addAll(threadController.findAll())

        coroutineScope.launch {
            eventBus.subscribe<ThreadCreateEvent> {
                launch {
                    val threadModelMapper = threadController.threadModelMapper(it.thread)
                    withContext(Dispatchers.JavaFx) {
                        items.add(threadModelMapper)
                    }
                }
            }
        }

        coroutineScope.launch {
            eventBus.subscribe<ThreadDeleteEvent> { event ->
                launch(Dispatchers.JavaFx) {
                    tableView.items.removeIf { it.threadId == event.threadId }
                }
            }
        }

        coroutineScope.launch {
            eventBus.subscribe<ThreadClearEvent> {
                launch(Dispatchers.JavaFx) {
                    tableView.items.clear()
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
                tableRow.contextMenuProperty().bind(tableRow.emptyProperty()
                    .map { empty -> if (empty) null else contextMenu })
                tableRow
            }
            column("Title", ThreadModel::titleProperty) {
                prefWidth = 350.0
            }
            column("URL", ThreadModel::linkProperty) {
                prefWidth = 350.0
            }
            column("Count", ThreadModel::totalProperty)
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

    private fun selectPosts(threadId: String) {
        find<ThreadSelectionTableView>(mapOf(ThreadSelectionTableView::threadId to threadId)).openModal()
            ?.apply {
                minWidth = 600.0
                minHeight = 400.0
                width = 800.0
                height = 600.0
            }
    }
}