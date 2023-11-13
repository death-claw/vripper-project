package me.vripper.gui.view.actionbar

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Orientation
import javafx.scene.control.ButtonType
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.gui.Styles
import me.vripper.gui.controller.GlobalStateController
import me.vripper.gui.controller.PostController
import me.vripper.gui.view.tables.PostsTableView
import tornadofx.*

class DownloadActionsView : View() {

    private val postController: PostController by inject()
    private val globalStateController: GlobalStateController by inject()
    private val postsTableView: PostsTableView by inject()
    private val downloadActiveProperty = SimpleBooleanProperty(true)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        downloadActiveProperty.bind(globalStateController.globalState.runningProperty.greaterThan(0))
    }

    override val root = hbox {
        button("Start All") {
            imageview("end.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Start downloads [Ctrl+S]")
            shortcut(KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN))
            disableWhen(downloadActiveProperty)
            action {
                postController.startAll()
            }
        }
        button("Stop All") {
            setPrefSize(32.0, 32.0)
            imageview("stop.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Stops all running downloads [Ctrl+Q]")
            shortcut(KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN))
            disableWhen(downloadActiveProperty.not())
            action {
                postController.stopAll()
            }
        }
        separator(Orientation.VERTICAL)
        button("Start") {
            setPrefSize(32.0, 32.0)
            imageview("play.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
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
        button("Stop") {
            setPrefSize(32.0, 32.0)
            imageview("pause.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
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
        button("Delete") {
            setPrefSize(32.0, 32.0)
            imageview("trash.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
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
        button("Clear") {
            setPrefSize(32.0, 32.0)
            imageview("broom.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            tooltip("Clear all finished downloads [Ctrl+Del]")
            shortcut(KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN))
            action {
                confirm("Clean finished posts", "Confirm removal of finished posts", ButtonType.YES, ButtonType.NO) {
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
    }
}
