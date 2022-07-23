package me.mnlr.vripper.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import me.mnlr.vripper.controller.LogController
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.model.LogModel
import tornadofx.*

class LogTableView : View("Log") {

    private val logController: LogController by inject()
    private val eventBus: EventBus by di()

    lateinit var tableView: TableView<LogModel>

    var items: ObservableList<LogModel> = FXCollections.observableArrayList()

    init {
        items.addAll(logController.findAll())

        eventBus.flux()
            .doOnNext { event ->
                if (event!!.kind.equals(Event.Kind.LOG_EVENT_UPDATE)) {
                    logController.find(event.data as Long)
                        .ifPresent {
                            // search
                            val find = items
                                .find { threadModel -> threadModel.id == it.id }
                            if (find != null) {
                                find.apply {
                                    status = it.status
                                    message = it.message
                                }
                            } else {
                                items.add(it)
                            }
                        }
                } else if (event.kind.equals(Event.Kind.LOG_EVENT_REMOVE)) {
                    items.removeIf { it.id == event.data }
                }
            }
            .subscribe()
    }

    override fun onDock() {
        tableView.prefHeightProperty().bind(root.heightProperty())
    }

    override val root = vbox {
        tableView = tableview(items) {
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            column("Time", LogModel::timeProperty) {
                sortOrder.add(this)
            }
            column("Type", LogModel::typeProperty)
            column("Status", LogModel::statusProperty)
            column("Message", LogModel::messageProperty)
        }
    }
}
