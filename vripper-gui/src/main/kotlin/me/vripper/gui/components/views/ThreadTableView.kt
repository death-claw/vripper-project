package me.vripper.gui.components.views

import atlantafx.base.theme.Styles
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.javafx.asFlow
import me.vripper.gui.components.fragments.ColumnSelectionFragment
import me.vripper.gui.components.fragments.ThreadSelectionTableFragment
import me.vripper.gui.controller.ThreadController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.model.ThreadModel
import me.vripper.gui.utils.openLink
import me.vripper.services.IAppEndpointService
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class ThreadTableView : View() {

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val threadController: ThreadController by inject()
    private val widgetsController: WidgetsController by inject()
    private val localAppEndpointService: IAppEndpointService by di("localAppEndpointService")
    private val remoteAppEndpointService: IAppEndpointService by di("remoteAppEndpointService")
    private val tableView: TableView<ThreadModel>
    private val items: ObservableList<ThreadModel> = FXCollections.observableArrayList()
    private val jobs = mutableListOf<Job>()

    override val root = vbox {}

    init {
        coroutineScope.launch {
            GuiEventBus.events.collect { event ->
                when (event) {
                    is GuiEventBus.LocalSession -> {
                        threadController.appEndpointService = localAppEndpointService
                        connect()
                    }

                    is GuiEventBus.RemoteSession -> {
                        threadController.appEndpointService = remoteAppEndpointService
                        connect()
                    }

                    is GuiEventBus.ChangingSession -> {
                        jobs.forEach { it.cancel() }
                        jobs.clear()
                    }
                }
            }
        }

        with(root) {
            tableView = tableview(items) {
                addClass(Styles.DENSE)
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
                        graphic = FontIcon.of(Feather.MENU)
                    }

                    val urlItem = MenuItem("Open link").apply {
                        setOnAction {
                            openLink(tableRow.item.link)
                        }
                        graphic = FontIcon.of(Feather.LINK)
                    }

                    val deleteItem = MenuItem("Delete").apply {
                        setOnAction {
                            deleteSelected()
                        }
                        graphic = FontIcon.of(Feather.TRASH)
                    }

                    val contextMenu = ContextMenu()
                    contextMenu.items.addAll(selectItem, urlItem, SeparatorMenuItem(), deleteItem)
                    tableRow.contextMenuProperty()
                        .bind(tableRow.emptyProperty().map { empty -> if (empty) null else contextMenu })
                    tableRow
                }
                contextMenu = ContextMenu()
                contextMenu.items.addAll(MenuItem("Setup columns").apply {
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
                    graphic = FontIcon.of(Feather.COLUMNS)
                }, SeparatorMenuItem(), MenuItem("Clear", FontIcon.of(Feather.TRASH_2)).apply {
                    setOnAction {
                        confirm(
                            "",
                            "Confirm removal of all threads",
                            ButtonType.YES,
                            ButtonType.NO,
                            owner = primaryStage,
                            title = "Clean threads"
                        ) {
                            coroutineScope.launch {
                                threadController.clearAll()
                            }
                        }
                    }
                })
                column("Title", ThreadModel::titleProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.threadsColumnsModel.titleProperty)
                    prefWidth = widgetsController.currentSettings.threadsColumnsWidthModel.title
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.threadsColumnsWidthModel.title = it as Double
                        }
                    }
                }
                column("URL", ThreadModel::linkProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.threadsColumnsModel.linkProperty)
                    prefWidth = widgetsController.currentSettings.threadsColumnsWidthModel.link
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.threadsColumnsWidthModel.link = it as Double
                        }
                    }
                }
                column("Count", ThreadModel::totalProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.threadsColumnsModel.countProperty)
                    prefWidth = widgetsController.currentSettings.threadsColumnsWidthModel.count
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.threadsColumnsWidthModel.count = it as Double
                        }
                    }
                }
            }
        }

        titleProperty.bind(items.sizeProperty.map {
            if (it.toLong() > 0) {
                "Threads (${it.toLong()})"
            } else {
                "Threads"
            }
        })
        tableView.prefHeightProperty().bind(root.heightProperty())
        tableView.placeholder = Label("Loading")
    }

    fun connect() {
        coroutineScope.launch {
            val list = async {
                threadController.findAll()
            }.await()
            runLater {
                items.clear()
                items.addAll(list)
                tableView.placeholder = Label("No content in table")
            }
        }
        coroutineScope.launch {
            threadController.onNewThread().collect {
                runLater {
                    items.add(it)
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            threadController.onUpdateThread().collect { thread ->
                runLater {
                    val threadModel = items.find { it.threadId == thread.threadId } ?: return@runLater
                    threadModel.total = thread.total
                    threadModel.title = thread.title
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            threadController.onDeleteThread().collect { threadId ->
                runLater {
                    tableView.items.removeIf { it.threadId == threadId }
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            threadController.onClearThreads().collect {
                runLater {
                    tableView.items.clear()
                }
            }
        }.also { jobs.add(it) }
    }

    private fun deleteSelected() {
        val threadIdList = tableView.selectionModel.selectedItems.map { it.threadId }
        confirm(
            "",
            "Confirm removal of ${threadIdList.size} thread${if (threadIdList.size > 1) "s" else ""}?",
            ButtonType.YES,
            ButtonType.NO,
            owner = primaryStage,
            title = "Clean threads"
        ) {
            coroutineScope.launch {
                threadController.delete(threadIdList)
            }
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