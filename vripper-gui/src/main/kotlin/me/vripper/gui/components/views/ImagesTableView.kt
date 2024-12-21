package me.vripper.gui.components.views

import atlantafx.base.theme.Styles
import atlantafx.base.theme.Tweaks
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.MouseButton
import javafx.util.Callback
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.javafx.asFlow
import me.vripper.entities.Status
import me.vripper.gui.components.cells.PreviewTableCell
import me.vripper.gui.components.cells.ProgressTableCell
import me.vripper.gui.components.cells.StatusTableCell
import me.vripper.gui.controller.ImageController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.model.ImageModel
import me.vripper.gui.utils.ActiveUICoroutines
import me.vripper.gui.utils.Preview
import me.vripper.gui.utils.openLink
import me.vripper.services.IAppEndpointService
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import kotlin.io.path.Path

class ImagesTableView : View("Photos") {

    private val tableView: TableView<ImageModel>
    private val imageController: ImageController by inject()
    private val widgetsController: WidgetsController by inject()
    private val localAppEndpointService: IAppEndpointService by di("localAppEndpointService")
    private val remoteAppEndpointService: IAppEndpointService by di("remoteAppEndpointService")
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val items: ObservableList<ImageModel> = FXCollections.observableArrayList()
    private var preview: Preview? = null

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {}

    init {
        if (widgetsController.currentSettings.localSession) {
            imageController.appEndpointService = localAppEndpointService
        } else {
            imageController.appEndpointService = remoteAppEndpointService
        }

        coroutineScope.launch {
            GuiEventBus.events.collect { event ->
                when (event) {
                    is GuiEventBus.LocalSession -> {
                        imageController.appEndpointService = localAppEndpointService
                    }

                    is GuiEventBus.RemoteSession -> {
                        imageController.appEndpointService = remoteAppEndpointService
                    }

                    is GuiEventBus.ChangingSession -> {
                        ActiveUICoroutines.images.forEach { it.cancelAndJoin() }
                        ActiveUICoroutines.images.clear()
                    }
                }
            }
        }

        with(root) {
            tableView = tableview(items) {
                isTableMenuButtonVisible = true
                addClass(Styles.DENSE, Tweaks.EDGE_TO_EDGE)
                setRowFactory {
                    val tableRow = TableRow<ImageModel>()
                    val urlItem = MenuItem("Open link").apply {
                        setOnAction {
                            openLink(tableRow.item.url)
                        }
                        graphic = FontIcon.of(Feather.LINK)
                    }
                    val contextMenu = ContextMenu()
                    contextMenu.items.addAll(urlItem)
                    tableRow.contextMenuProperty().bind(
                        tableRow.emptyProperty()
                            .map { empty -> if (empty) null else contextMenu })
                    tableRow
                }
                column("Preview", ImageModel::thumbUrlProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.previewProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.previewProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.preview
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.preview = it as Double
                        }
                    }
                    cellFactory = Callback {
                        val cell = PreviewTableCell<ImageModel>()
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
                            if (cell.tableRow.item != null && cell.tableRow.item.thumbUrl.isNotEmpty()) {
                                preview = Preview(
                                    currentStage!!,
                                    cell.tableRow.item.thumbUrl,
                                    Path(widgetsController.currentSettings.cachePath)
                                )
                                preview?.previewPopup?.apply {
                                    x = mouseEvent.screenX + 20
                                    y = mouseEvent.screenY + 10
                                }
                            }
                        }
                        cell
                    }
                }
                column("Index", ImageModel::indexProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.indexProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.indexProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.index
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.index = it as Double
                        }
                    }
                    sortOrder.add(this)
                    cellFactory = Callback {
                        TextFieldTableCell<ImageModel?, Number?>().apply { alignment = Pos.CENTER_LEFT }
                    }
                }
                column("Link", ImageModel::urlProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.linkProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.linkProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.link
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.link = it as Double
                        }
                    }
                    cellFactory = Callback {
                        TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                    }
                }
                column("Progress", ImageModel::progressProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.progressProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.progressProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.progress
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.progress = it as Double
                        }
                    }
                    cellFactory = Callback {
                        val cell = ProgressTableCell<ImageModel>()
                        cell.alignment = Pos.CENTER
                        cell.setOnMouseClick {
                            when (it.clickCount) {
                                1 -> {
                                    this@tableview.requestFocus()
                                    this@tableview.focusModel.focus(cell.tableRow.index)
                                    if (it.button.equals(MouseButton.PRIMARY)) {
                                        this@tableview.selectionModel.clearSelection()
                                        this@tableview.selectionModel.select(cell.tableRow.index)
                                    }
                                }
                            }
                        }
                        cell as TableCell<ImageModel, Number>
                    }
                }
                column("Filename", ImageModel::filenameProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.filenameProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.filenameProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.filename
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.filename = it as Double
                        }
                    }
                    cellFactory = Callback {
                        TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                    }
                }
                column("Status", ImageModel::statusProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.statusProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.statusProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.status
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.status = it as Double
                        }
                    }
                    cellFactory = Callback {
                        StatusTableCell()
                    }
                }
                column("Size", ImageModel::sizeProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.sizeProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.sizeProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.size
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.size = it as Double
                        }
                    }
                    cellFactory = Callback {
                        TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                    }
                }
                column("Downloaded", ImageModel::downloadedProperty) {
                    isVisible = widgetsController.currentSettings.imagesColumnsModel.downloadedProperty.get()
                    visibleProperty().onChange {
                        widgetsController.currentSettings.imagesColumnsModel.downloadedProperty.set(
                            it
                        )
                    }
                    prefWidth = widgetsController.currentSettings.imagesColumnsWidthModel.downloaded
                    coroutineScope.launch {
                        widthProperty().asFlow().debounce(200).collect {
                            widgetsController.currentSettings.imagesColumnsWidthModel.downloaded = it as Double
                        }
                    }
                    cellFactory = Callback {
                        TextFieldTableCell<ImageModel?, String?>().apply { alignment = Pos.CENTER_LEFT }
                    }
                }
            }
        }
        tableView.prefWidthProperty().bind(root.widthProperty())
        tableView.prefHeightProperty().bind(root.heightProperty())
        modalStage?.width = 550.0
    }

    fun setPostId(postId: Long?) {
        ActiveUICoroutines.images.forEach { it.cancel() }
        ActiveUICoroutines.images.clear()
        runLater {
            items.clear()
        }
        if (postId == null) {
            return
        }
        coroutineScope.launch {
            val list = coroutineScope.async {
                imageController.findImages(postId)
            }.await()
            runLater {
                items.addAll(list)
                tableView.sort()
                tableView.placeholder = Label("No content in table")
            }
        }
        coroutineScope.launch {
            imageController.onUpdateImages(postId).collect { image ->
                runLater {
                    val imageModel = items.find { it.id == image.id } ?: return@runLater

                    imageModel.size = image.size
                    imageModel.status = image.status.name
                    imageModel.filename = image.filename
                    imageModel.downloaded = image.downloaded
                    imageModel.progress = imageController.progress(
                        image.size, image.downloaded
                    )
                }
            }
        }.also { ActiveUICoroutines.images.add(it) }

        coroutineScope.launch {
            imageController.onStopped().collect {
                runLater {
                    items.forEach { imageModel ->
                        if (imageModel.status != Status.FINISHED.name) {
                            imageModel.status = Status.STOPPED.name
                        }
                    }
                }
            }
        }.also { ActiveUICoroutines.images.add(it) }
    }
}