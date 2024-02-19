package me.vripper.gui.view.tables

import javafx.scene.control.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import me.vripper.event.EventBus
import me.vripper.event.LogCreateEvent
import me.vripper.event.LogDeleteEvent
import me.vripper.event.LogUpdateEvent
import me.vripper.gui.controller.LogController
import me.vripper.gui.model.LogModel
import me.vripper.gui.view.popup.LogMessageView
import tornadofx.*

class LogTableView : View() {

    private val logController: LogController by inject()
    private val eventBus: EventBus by di()
    private lateinit var tableView: TableView<LogModel>
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var items: SortedFilteredList<LogModel>

    override fun onDock() {
        items = SortedFilteredList<LogModel>().bindTo(tableView)
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
        coroutineScope.launch {
            val list = coroutineScope.async {
                logController.findAll().sortedByDescending { it.time }
            }.await()
            runLater {
                items.addAll(list)
                tableView.placeholder = Label("No content in table")
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(LogCreateEvent::class).collect {
                val logModel = logController.mapper(it.logEntry)
                runLater {
                    items.add(logModel)
                }
            }
        }
        coroutineScope.launch {
            eventBus.events.filterIsInstance(LogUpdateEvent::class).collect {
                val logModel = logController.mapper(it.logEntry)
                val find = items.find { threadModel -> threadModel.id == logModel.id }
                if (find != null) {
                    runLater {
                        find.apply {
                            status = logModel.status
                            message = logModel.message
                        }
                    }
                }
            }
        }

        coroutineScope.launch {
            eventBus.events.filterIsInstance(LogDeleteEvent::class).collect { deleteEvent ->
                runLater {
                    items.items.removeIf { it.id in deleteEvent.deleted }
                }
            }
        }
    }

    override val root = vbox {
        tableView = tableview {
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
            column("Time", LogModel::timeProperty) {
                id = "time"
                prefWidth = 150.0
                sortType = TableColumn.SortType.DESCENDING
            }
            column("Type", LogModel::typeProperty) {
                id = "type"
                isSortable = false
            }
            column("Status", LogModel::statusProperty) {
                id = "status"
                isSortable = false
            }
            column("Message", LogModel::messageProperty) {
                id = "message"
                prefWidth = 300.0
                isSortable = false
            }
        }
    }

    private fun openLog(item: LogModel) {
        find<LogMessageView>(mapOf(LogMessageView::logModel to item)).openModal()?.apply {
            minWidth = 600.0
            minHeight = 400.0
        }
    }
}
