package me.vripper.gui.components.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ButtonType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlinx.coroutines.*
import me.vripper.gui.VripperGuiApplication
import me.vripper.gui.components.fragments.AboutFragment
import me.vripper.gui.components.fragments.AddLinksFragment
import me.vripper.gui.components.fragments.SettingsFragment
import me.vripper.gui.controller.GlobalStateController
import me.vripper.gui.controller.PostController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.utils.openLink
import me.vripper.utilities.ApplicationProperties
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class MenuBarView : View() {

    private val globalStateController: GlobalStateController by inject()
    private val postController: PostController by inject()
    private val postsTableView: PostsTableView by inject()
    private val downloadActiveProperty = SimpleBooleanProperty(false)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val widgetsController: WidgetsController by inject()

    override val root = menubar {
        menu("File") {
            item("Add links", KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN)).apply {
                graphic = FontIcon.of(Feather.PLUS)
                action {
                    find<AddLinksFragment>().apply {
                        input.clear()
                    }.openModal()
                }
            }
            separator()
            item("Start All", KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)).apply {
                graphic = FontIcon.of(Feather.SKIP_FORWARD)
                disableWhen(downloadActiveProperty)
                action {
                    postController.startAll()
                }
            }
            item("Stop All", KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)).apply {
                graphic = FontIcon.of(Feather.SQUARE)
                disableWhen(downloadActiveProperty.not())
                action {
                    postController.stopAll()
                }
            }
            separator()
            item("Start", KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)).apply {
                graphic = FontIcon.of(Feather.PLAY)
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
                graphic = FontIcon.of(Feather.SQUARE)
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
                graphic = FontIcon.of(Feather.EDIT)
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
                graphic = FontIcon.of(Feather.TRASH)
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
                graphic = FontIcon.of(Feather.TRASH_2)
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
            separator()
            item("Settings", KeyCodeCombination(KeyCode.S)).apply {
                graphic = FontIcon.of(Feather.SETTINGS)
                action {
                    find<SettingsFragment>().openModal()?.apply {
                        minWidth = 700.0
                        minHeight = 400.0
                    }
                }
            }
            separator()
            item("Exit", KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN)).apply {
                graphic = FontIcon.of(Feather.X_SQUARE)
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
                "Status Bar", KeyCodeCombination(KeyCode.F8)
            ).bind(widgetsController.currentSettings.visibleStatusBarPanelProperty)
            checkmenuitem(
                "Dark mode"
            ).bind(widgetsController.currentSettings.darkModeProperty)
        }
        menu("Help") {
            item("Check for updates").apply {
                graphic = FontIcon.of(Feather.REFRESH_CCW)
                action {
                    val latestVersion = ApplicationProperties.latestVersion()
                    val currentVersion = ApplicationProperties.VERSION

                    if (latestVersion > currentVersion) {
                        information(
                            header = "",
                            content = "A newer version of VRipper is available \nLatest version is $latestVersion\nDo you want to go to the release page?",
                            title = "VRipper updates",
                            buttons = arrayOf(ButtonType.YES, ButtonType.NO),
                            owner = primaryStage,
                        ) {
                            if (it == ButtonType.YES) {
                                openLink("https://github.com/death-claw/vripper-project/releases/tag/$latestVersion")
                            }
                        }
                    } else {
                        information(
                            header = "",
                            content = "You are running the latest version of VRipper.",
                            title = "VRipper updates",
                            owner = primaryStage
                        )
                    }
                }
            }
            separator()
            item("About").apply {
                graphic = FontIcon.of(Feather.INFO)
                action {
                    find<AboutFragment>().openModal()?.apply {
                        this.minWidth = 550.0
                        this.minHeight = 200.0
                    }
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