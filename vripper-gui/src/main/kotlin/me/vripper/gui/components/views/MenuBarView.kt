package me.vripper.gui.components.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ButtonType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.vripper.gui.VripperGuiApplication
import me.vripper.gui.components.fragments.AboutFragment
import me.vripper.gui.components.fragments.AddLinksFragment
import me.vripper.gui.components.fragments.SettingsFragment
import me.vripper.gui.controller.GlobalStateController
import me.vripper.gui.controller.PostController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.utils.openLink
import me.vripper.utilities.ApplicationProperties
import tornadofx.*

class MenuBarView : View() {

    private val globalStateController: GlobalStateController by inject()
    private val postController: PostController by inject()
    private val postsTableView: PostsTableView by inject()
    private val downloadActiveProperty = SimpleBooleanProperty(true)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val widgetsController: WidgetsController by inject()

    init {
        downloadActiveProperty.bind(globalStateController.globalState.runningProperty.greaterThan(0))
    }

    override val root = menubar {
        menu("File") {
            item("Add links", KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN)).apply {
                graphic = imageview("plus.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                action {
                    find<AddLinksFragment>().apply {
                        input.clear()
                    }.openModal()
                }
            }
            separator()
            item("Start All", KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)).apply {
                graphic = imageview("end.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                disableWhen(downloadActiveProperty)
                action {
                    postController.startAll()
                }
            }
            item("Stop All", KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)).apply {
                graphic = imageview("stop.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                disableWhen(downloadActiveProperty.not())
                action {
                    postController.stopAll()
                }
            }
            item("Start", KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)).apply {
                graphic = imageview("play.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.startSelected()
                }
            }
            item("Stop", KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)).apply {
                graphic = imageview("pause.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.stopSelected()
                }
            }
            item("Rename", KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN)).apply {
                graphic = imageview("edit.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.renameSelected()
                }
            }
            item("Delete", KeyCodeCombination(KeyCode.DELETE)).apply {
                graphic = imageview("trash.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                enableWhen(
                    postsTableView.tableView.selectionModel.selectedItems.sizeProperty.greaterThan(
                        0
                    )
                )
                action {
                    postsTableView.deleteSelected()
                }
            }
            item("Clear", KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN)).apply {
                graphic = imageview("broom.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                action {
                    confirm(
                        "Clean finished posts", "Confirm removal of finished posts", ButtonType.YES, ButtonType.NO
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
            separator()
            item("Settings", KeyCodeCombination(KeyCode.S)).apply {
                graphic = imageview("settings.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                action {
                    find<SettingsFragment>().openModal()?.apply {
                        minWidth = 600.0
                        minHeight = 400.0
                    }
                }
            }
            separator()
            item("Exit", KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN)).apply {
                graphic = imageview("close.png") {
                    fitWidth = 18.0
                    fitHeight = 18.0
                }
                action {
                    VripperGuiApplication.APP_INSTANCE.stop()
                }
            }
        }
        menu("View") {
            checkmenuitem(
                "Toolbar", KeyCodeCombination(KeyCode.F6)
            ).bind(widgetsController.currentSettings.visibleToolbarPanelProperty)
            checkmenuitem(
                "Info Panel", KeyCodeCombination(KeyCode.F7)
            ).bind(widgetsController.currentSettings.visibleInfoPanelProperty)
            checkmenuitem(
                "Status bar", KeyCodeCombination(KeyCode.F8)
            ).bind(widgetsController.currentSettings.visibleStatusBarPanelProperty)
        }
        menu("Help") {
            item("Check for updates").apply {
                imageview("available-updates.png") { fitWidth = 18.0; fitHeight = 18.0 }
                action {
                    val latestVersion = ApplicationProperties.latestVersion()
                    val currentVersion = ApplicationProperties.VERSION

                    if (latestVersion > currentVersion) {
                        information(
                            header = "Please update to the latest version of VRipper v$latestVersion",
                            content = "Do you want to go to the release page ?",
                            title = "VRipper updates",
                            buttons = arrayOf(ButtonType.YES, ButtonType.NO),
                        ) {
                            if (it == ButtonType.YES) {
                                openLink("https://github.com/death-claw/vripper-project/releases/tag/$latestVersion")
                            }
                        }
                    } else {
                        information(
                            header = "No updates have been found",
                            content = "You are running the latest version of VRipper.",
                            title = "VRipper updates"
                        )
                    }
                }
            }
            separator()
            item("About").apply {
                imageview("about.png") { fitWidth = 18.0; fitHeight = 18.0 }
                action {
                    find<AboutFragment>().openModal()?.apply {
                        this.minWidth = 550.0
                        this.minHeight = 200.0
                    }
                }
            }
        }
    }
}