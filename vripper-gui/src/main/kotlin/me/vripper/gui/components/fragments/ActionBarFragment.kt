package me.vripper.gui.components.fragments

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Orientation
import javafx.scene.control.ButtonType
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlinx.coroutines.*
import me.vripper.gui.components.views.PostsTableView
import me.vripper.gui.controller.GlobalStateController
import me.vripper.gui.controller.PostController
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class ActionBarFragment : Fragment() {

    private val downloadActiveProperty = SimpleBooleanProperty(true)
    private val postController: PostController by inject()
    private val globalStateController: GlobalStateController by inject()
    private val postsTableView: PostsTableView by inject()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val root = toolbar {
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
                postController.startAll()
            }
        }
        button("Stop All", FontIcon.of(Feather.SQUARE)) {
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Stops all running downloads [Ctrl+Q]")
            shortcut(KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN))
            disableWhen(downloadActiveProperty.not())
            action {
                postController.stopAll()
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
                        val clearPosts = postController.clearPosts().await()
                        coroutineScope.launch {
                            runLater {
                                postsTableView.tableView.items.removeIf { clearPosts.contains(it.postId) }
                            }
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
                find<SettingsFragment>().openModal()?.apply {
                    minWidth = 700.0
                    minHeight = 400.0
                }
            }
        }
    }

    override fun onDock() {
        downloadActiveProperty.bind(globalStateController.globalState.runningProperty.greaterThan(0))
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}