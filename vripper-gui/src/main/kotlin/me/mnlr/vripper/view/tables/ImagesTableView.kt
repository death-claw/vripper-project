package me.mnlr.vripper.view.tables

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.TableView
import javafx.scene.input.MouseButton
import me.mnlr.vripper.controller.ImageController
import me.mnlr.vripper.event.Event
import me.mnlr.vripper.event.EventBus
import me.mnlr.vripper.model.ImageModel
import tornadofx.*

class ImagesTableView : Fragment("Photos") {

    private lateinit var tableView: TableView<ImageModel>
    private val imageController: ImageController by inject()
    private val eventBus: EventBus by di()
    private var items: ObservableList<ImageModel> = FXCollections.observableArrayList()
    val postId: String by param()

    override fun onDock() {
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 600.0

        runLater {
            items.addAll(imageController.findImages(postId))
            tableView.sort()

            val disposable = eventBus.flux()
                .filter { it!!.kind == Event.Kind.IMAGE_UPDATE }
                .subscribe { event ->
                    imageController
                        .findImageById(event!!.data as Long)
                        .filter { it.postId == postId }
                        .ifPresent {
                            // search
                            val find = items
                                .find { image -> image.id == it.id }
                            if (find != null) {
                                find.apply {
                                    progress = it.progress
                                    status = it.status
                                }
                            } else {
                                items.add(it)
                            }
                        }
                }

            whenUndocked {
                disposable.dispose()
            }
        }
    }

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        tableView = tableview(items) {
            column("Index", ImageModel::indexProperty) {
                sortOrder.add(this)
            }
            column("Link", ImageModel::urlProperty) {
                prefWidth = 200.0
            }
            column("Progress", ImageModel::progressProperty) {
                cellFormat {
                    addClass(Stylesheet.progressBarTableCell)
                    graphic = cache {
                        progressbar(itemProperty().doubleBinding { it?.toDouble() ?: 0.0 }) {
                            setOnMouseClicked {
                                when (it.clickCount) {
                                    1 -> {
                                        this@tableview.requestFocus()
                                        this@tableview.focusModel.focus(this@cellFormat.tableRow.index)
                                        if (it.isControlDown && it.button.equals(MouseButton.PRIMARY)) {
                                            if (this@tableview.selectionModel.isSelected(this@cellFormat.tableRow.index)) {
                                                this@tableview.selectionModel.clearSelection(this@cellFormat.tableRow.index)
                                            } else {
                                                this@tableview.selectionModel.select(this@cellFormat.tableRow.index)
                                            }
                                        } else if (it.button.equals(MouseButton.PRIMARY)) {
                                            this@tableview.selectionModel.clearSelection()
                                            this@tableview.selectionModel.select(this@cellFormat.tableRow.index)
                                        }
                                    }
                                }
                            }
                            useMaxWidth = true
                        }
                    }
                }
            }
            column("Status", ImageModel::statusProperty)
        }
    }
}