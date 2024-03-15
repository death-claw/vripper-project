package me.vripper.gui.components.views

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.control.ButtonType
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import me.vripper.delegate.LoggerDelegate
import me.vripper.gui.VripperGuiApplication
import me.vripper.gui.components.fragments.AboutFragment
import me.vripper.gui.components.fragments.AddLinksFragment
import me.vripper.gui.components.fragments.SessionFragment
import me.vripper.gui.components.fragments.SettingsFragment
import me.vripper.gui.controller.PostController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.services.GrpcEndpointService
import me.vripper.gui.utils.openLink
import me.vripper.services.AppEndpointService
import me.vripper.services.IAppEndpointService
import me.vripper.utilities.ApplicationProperties
import org.kordamp.ikonli.feather.Feather
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class MenuBarView : View() {
    private val logger by LoggerDelegate()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val downloadActiveProperty = SimpleBooleanProperty(false)
    private val postsTableView: PostsTableView by inject()
    private val widgetsController: WidgetsController by inject()
    private val postController: PostController by inject()
    private val grpcEndpointService: GrpcEndpointService by di("remoteAppEndpointService")
    private val localEndpointService: AppEndpointService by di("localAppEndpointService")
    private val running = SimpleIntegerProperty(0)
    private val jobs = mutableListOf<Job>()

    override val root = menubar {}

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
                        coroutineScope.launch {
                            postController.startAll()
                        }
                    }
                }
                item("Stop All", KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)).apply {
                    graphic = FontIcon.of(Feather.SQUARE)
                    disableWhen(downloadActiveProperty.not())
                    action {
                        coroutineScope.launch {
                            postController.stopAll()
                        }
                    }
                }
                separator()
                item(
                    "Start", KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
                ).apply {
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
                item(
                    "Stop", KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
                ).apply {
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
                                val clearPosts = async { postController.clearPosts() }.await()
                                runLater {
                                    postsTableView.tableView.items.removeIf { clearPosts.contains(it.postId) }
                                }
                            }
                        }
                    }
                }
                separator()
                item("Settings", KeyCodeCombination(KeyCode.S)).apply {
                    graphic = FontIcon.of(Feather.SETTINGS)
                    action {
                        find<SettingsFragment>().openModal(owner = primaryStage)?.apply {
                            minWidth = 700.0
                            minHeight = 400.0
                        }
                    }
                }
                separator()
                item("Change session", KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)) {
                    graphic = FontIcon.of(Feather.LINK_2)
                    action {
                        find<SessionFragment>(mapOf(SessionFragment::component to find<AppView>())).openModal()
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
                            this.minWidth = 625.0
                            this.minHeight = 200.0
                        }
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