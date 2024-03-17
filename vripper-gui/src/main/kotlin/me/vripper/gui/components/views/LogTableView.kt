package me.vripper.gui.components.views

import atlantafx.base.theme.Styles
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.javafx.asFlow
import me.vripper.gui.components.fragments.ColumnSelectionFragment
import me.vripper.gui.components.fragments.LogMessageFragment
import me.vripper.gui.controller.LogController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.model.LogModel
import me.vripper.services.IAppEndpointService
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class LogTableView : View() {

    private val logController: LogController by inject()
    private val widgetsController: WidgetsController by inject()
    private val localAppEndpointService: IAppEndpointService by di("localAppEndpointService")
    private val remoteAppEndpointService: IAppEndpointService by di("remoteAppEndpointService")
    private val tableView: TableView<LogModel>
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val items: ObservableList<LogModel> = FXCollections.observableArrayList()
    private val jobs = mutableListOf<Job>()

    override val root = vbox {}

    init {
        coroutineScope.launch {
            GuiEventBus.events.collect { event ->
                when (event) {
                    is GuiEventBus.LocalSession -> {
                        logController.appEndpointService = localAppEndpointService
                        connect()
                    }

                    is GuiEventBus.RemoteSession -> {
                        logController.appEndpointService = remoteAppEndpointService
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
                selectionModel.selectionMode = SelectionMode.SINGLE
                setRowFactory {
                    val tableRow = TableRow<LogModel>()
                    tableRow.setOnMouseClicked {
                        if (it.clickCount == 2 && tableRow.item != null) {
                            openLog(tableRow.item)
                        }
                    }
                    tableRow
                }
                contextMenu = ContextMenu()
                contextMenu.items.addAll(MenuItem("Setup columns").apply {
                    setOnAction {
                        find<ColumnSelectionFragment>(
                            mapOf(
                                ColumnSelectionFragment::map to mapOf(
                                    Pair(
                                        "Time", widgetsController.currentSettings.logsColumnsModel.timeProperty
                                    ),
                                    Pair(
                                        "Type", widgetsController.currentSettings.logsColumnsModel.typeProperty
                                    ),
                                    Pair(
                                        "Status", widgetsController.currentSettings.logsColumnsModel.statusProperty
                                    ),
                                    Pair(
                                        "Message", widgetsController.currentSettings.logsColumnsModel.messageProperty
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
                            "Are you sure you want to clear the logs",
                            ButtonType.YES,
                            ButtonType.NO,
                            owner = primaryStage,
                            title = "Clear logs"
                        ) {
                            coroutineScope.launch {
                                coroutineScope {
                                    async { logController.clear() }.await()
                                    runLater {
                                        items.clear()
                                    }
                                }
                            }
                        }
                    }
                })
                column("Time", LogModel::timeProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.logsColumnsModel.timeProperty)
                    id = "time"
                    prefWidth = widgetsController.currentSettings.logsColumnsWidthModel.time
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.logsColumnsWidthModel.time = it as Double
                        }
                    }
                    sortType = TableColumn.SortType.DESCENDING
                }
                column("Type", LogModel::typeProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.logsColumnsModel.typeProperty)
                    prefWidth = widgetsController.currentSettings.logsColumnsWidthModel.type
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.logsColumnsWidthModel.type = it as Double
                        }
                    }
                    id = "type"
                    isSortable = false
                }
                column("Status", LogModel::statusProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.logsColumnsModel.statusProperty)
                    prefWidth = widgetsController.currentSettings.logsColumnsWidthModel.status
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.logsColumnsWidthModel.status = it as Double
                        }
                    }
                    id = "status"
                    isSortable = false
                }
                column("Message", LogModel::messageProperty) {
                    visibleProperty().bind(widgetsController.currentSettings.logsColumnsModel.messageProperty)
                    prefWidth = widgetsController.currentSettings.logsColumnsWidthModel.message
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.logsColumnsWidthModel.message = it as Double
                        }
                    }
                    id = "message"
                    isSortable = false
                }
            }
        }
        titleProperty.bind(items.sizeProperty.map {
            if (it.toLong() > 0) {
                "Log (${it.toLong()})"
            } else {
                "Log"
            }
        })
        tableView.prefHeightProperty().bind(root.heightProperty())
        tableView.placeholder = Label("Loading")
        tableView.sortOrder.add(tableView.columns.first { it.id == "time" })
    }

    private fun connect() {
        coroutineScope.launch {
            val list = coroutineScope.async {
                logController.findAll().sortedByDescending { it.time }
            }.await()
            runLater {
                items.clear()
                items.addAll(list)
                tableView.placeholder = Label("No content in table")
            }
        }

        coroutineScope.launch {
            logController.onNewLog().collect {
                runLater {
                    items.add(it)
                    tableView.sort()
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            logController.onUpdateLog().collect {
                val find = items.find { threadModel -> threadModel.id == it.id }
                if (find != null) {
                    runLater {
                        find.apply {
                            status = it.status.name
                            message = it.message
                        }
                    }
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            logController.onDeleteLogs().collect { deletedId ->
                runLater {
                    items.removeIf { it.id == deletedId }
                }
            }
        }.also { jobs.add(it) }
    }

    private fun openLog(item: LogModel) {
        find<LogMessageFragment>(mapOf(LogMessageFragment::logModel to item)).openModal()?.apply {
            minWidth = 600.0
            minHeight = 400.0
        }
    }
}
