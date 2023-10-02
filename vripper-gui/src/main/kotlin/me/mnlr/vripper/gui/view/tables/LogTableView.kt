package me.mnlr.vripper.gui.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableView
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.gui.controller.LogController
import me.mnlr.vripper.gui.model.LogModel
import me.mnlr.vripper.gui.view.FxScheduler
import tornadofx.*

class LogTableView : View() {

    private val logController: LogController by inject()
    private val eventBus: EventBus by di()

    lateinit var tableView: TableView<LogModel>

    var items: ObservableList<LogModel> = FXCollections.observableArrayList()

    init {
        titleProperty.bind(items.sizeProperty.map {
            if (it.toLong() > 0) {
                "Log (${it.toLong()})"
            } else {
                "Log"
            }
        })
        items.addAll(logController.findAll())

        eventBus
            .flux()
            .filter { it!!.kind == Event.Kind.LOG_EVENT_UPDATE }
            .map { logController.find(it.data as Long) }
            .filter { it.isPresent }
            .map { it.get() }
            .publishOn(FxScheduler)
            .doOnNext {
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
            }.subscribe()

        eventBus
            .flux()
            .filter { it.kind == Event.Kind.LOG_EVENT_REMOVE }
            .publishOn(FxScheduler)
            .doOnNext() { event ->
                items.removeIf { it.id == event.data }
            }.subscribe()
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
