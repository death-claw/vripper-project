package me.vripper.gui.components.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.geometry.Orientation
import javafx.scene.control.ButtonType
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import me.vripper.delegate.LoggerDelegate
import me.vripper.gui.components.fragments.AddLinksFragment
import me.vripper.gui.components.fragments.SettingsFragment
import me.vripper.gui.controller.PostController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.services.GrpcEndpointService
import me.vripper.services.AppEndpointService
import me.vripper.services.IAppEndpointService
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class ActionBarView : View() {
    private val logger by LoggerDelegate()
    private val downloadActiveProperty = SimpleBooleanProperty(true)
    private val postController: PostController by inject()
    private val postsTableView: PostsTableView by inject()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val grpcEndpointService: GrpcEndpointService by di("remoteAppEndpointService")
    private val localEndpointService: AppEndpointService by di("localAppEndpointService")
    private val running = SimpleIntegerProperty(0)
    private val jobs = mutableListOf<Job>()

    override val root = toolbar {}

    init {
        coroutineScope.launch {
            GuiEventBus.events.collect { event ->
                when (event) {
                    is GuiEventBus.LocalSession -> {
                        connect(localEndpointService)
                    }

                    is GuiEventBus.RemoteSession -> {
                        connect(grpcEndpointService)
                    }

                    is GuiEventBus.ChangingSession -> {
                        jobs.forEach { it.cancel() }
                        jobs.clear()
                    }
                }
            }
        }
        with(root) {
            id = "action_toolbar"
            padding = insets(all = 5)
            button("Add links", FontIcon.of(Feather.PLUS)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Add links [Ctrl+L]")
                shortcut(KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN))
                action {
                    find<AddLinksFragment>().apply {
                        input.clear()
                    }.openModal()
                }
            }
            separator(Orientation.VERTICAL)

            button("Start All", FontIcon.of(Feather.SKIP_FORWARD)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Start downloads [Ctrl+S]")
                shortcut(KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN))
                disableWhen(downloadActiveProperty)
                action {
                    coroutineScope.launch {
                        postController.startAll()
                    }
                }
            }
            button("Stop All", FontIcon.of(Feather.SQUARE)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Stops all running downloads [Ctrl+Q]")
                shortcut(KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN))
                disableWhen(downloadActiveProperty.not())
                action {
                    coroutineScope.launch {
                        postController.stopAll()
                    }
                }
            }
            separator(Orientation.VERTICAL)
            button("Start", FontIcon.of(Feather.PLAY)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Start downloads for selected [Ctrl+Shift+S]")
                shortcut(KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.startSelected()
                }
            }
            button("Stop", FontIcon.of(Feather.SQUARE)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Stop downloads for selected [Ctrl+Shift+Q]")
                shortcut(KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.stopSelected()
                }
            }
            button("Rename", FontIcon.of(Feather.EDIT)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Rename selected [Ctrl+R]")
                shortcut(KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN))
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.renameSelected()
                }
            }
            button("Bulk rename", FontIcon.of(Feather.EDIT_3)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Bulk rename selected [Ctrl+Shift+R]")
                shortcut(KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN))
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.bulkRenameSelected()
                }
            }
            button("Delete", FontIcon.of(Feather.TRASH)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Delete selected posts [Del]")
                shortcut(KeyCodeCombination(KeyCode.DELETE))
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.deleteSelected()
                }
            }
            separator(Orientation.VERTICAL)
            button("Clear", FontIcon.of(Feather.TRASH_2)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Clear all finished downloads [Ctrl+Del]")
                shortcut(KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN))
                action {
                    confirm(
                        "",
                        "Confirm removal of finished posts?",
                        ButtonType.YES,
                        ButtonType.NO,
                        owner = primaryStage,
                        title = "Clean finished posts"
                    ) {
                        coroutineScope.launch {
                            val clearPosts = async { postController.clearPosts() }.await()
                            runLater {
                                postsTableView.tableView.items.removeIf { clearPosts.contains(it.postId) }
                            }
                        }
                    }
                }
            }

            button("Settings", FontIcon.of(Feather.SETTINGS)) {
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Open settings menu [S]")
                shortcut(KeyCodeCombination(KeyCode.S))
                action {
                    find<SettingsFragment>().openModal(owner = primaryStage)?.apply {
                        minWidth = 700.0
                        minHeight = 400.0
                    }
                }
            }
        }
        downloadActiveProperty.bind(running.greaterThan(0))
    }

    private fun connect(appEndpointService: IAppEndpointService) {
        coroutineScope.launch {
            appEndpointService.onQueueStateUpdate().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.collect {
                runLater {
                    running.set(it.running)
                }
            }
        }.also { jobs.add(it) }
    }
}