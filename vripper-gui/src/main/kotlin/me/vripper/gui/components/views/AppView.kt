package me.vripper.gui.components.views

import atlantafx.base.theme.CupertinoDark
import atlantafx.base.theme.CupertinoLight
import javafx.application.Application
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.*
import me.vripper.gui.components.fragments.ActionBarFragment
import me.vripper.gui.components.fragments.PostInfoPanelFragment
import me.vripper.gui.components.fragments.StatusBarFragment
import me.vripper.gui.controller.GlobalStateController
import me.vripper.gui.controller.MainController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.listener.GuiStartupLister
import me.vripper.gui.model.PostModel
import tornadofx.*

class AppView : View() {

    override val root = VBox()
    private val mainController: MainController by inject()
    private val postsTableView: PostsTableView by inject()
    private val menuBarView: MenuBarView by inject()
    private val mainView: MainView by inject()
    private val widgetsController: WidgetsController by inject()
    private val globalStateController: GlobalStateController by inject()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var splitpane: SplitPane
    private var lastSelectedInfoPanelTab = 0

    init {
        title = "VRipper ${mainController.version}"
        with(root) {
            style {
                fontFamily = "Inter"
            }
            add(menuBarView)
            if (widgetsController.currentSettings.visibleToolbarPanel) {
                add(find<ActionBarFragment>())
            }
            splitpane = splitpane {
                add(mainView)
                if (widgetsController.currentSettings.visibleInfoPanel) {
                    add(buildInfoPanel(null))
                    setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
                }
                orientation = Orientation.VERTICAL
                vboxConstraints { vGrow = Priority.ALWAYS }
            }
            if (widgetsController.currentSettings.visibleStatusBarPanel) {
                add(find<StatusBarFragment>())
            }
            prefWidth = widgetsController.currentSettings.width
            prefHeight = widgetsController.currentSettings.height
        }
    }

    override fun onDock() {
        globalStateController.init()
        GuiStartupLister().run()
        coroutineScope.launch {
            var lastDividerPosition = widgetsController.currentSettings.infoPanelDividerPosition
            while (isActive) {
                if (widgetsController.currentSettings.visibleInfoPanel) {
                    val dividerPosition = splitpane.dividerPositions.firstOrNull()
                    if (dividerPosition != null && lastDividerPosition != dividerPosition) {
                        lastDividerPosition = dividerPosition
                        widgetsController.currentSettings.infoPanelDividerPosition = lastDividerPosition
                        widgetsController.update()
                    }
                }
                delay(1_000)
            }
        }
        postsTableView.tableView.selectionModel.selectedItemProperty().onChange {
            if (widgetsController.currentSettings.visibleInfoPanel) {
                splitpane.items.removeIf { it.id == "postinfo_panel" }
                splitpane.add(buildInfoPanel(it))
                splitpane.setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
            }
        }
        mainView.root.selectionModel.selectedIndexProperty().onChange {
            if (it == 0 && widgetsController.currentSettings.visibleInfoPanel) {
                val selectedItem = postsTableView.tableView.selectedItem
                splitpane.add(buildInfoPanel(selectedItem))
                splitpane.setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
            } else {
                splitpane.items.removeIf { it.id == "postinfo_panel" }
            }
        }
        widgetsController.currentSettings.visibleInfoPanelProperty.onChange {
            if (mainView.root.selectionModel.selectedIndex != 0) return@onChange
            if (it) {
                val selectedItem = postsTableView.tableView.selectedItem
                splitpane.add(buildInfoPanel(selectedItem))
                splitpane.setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
            } else {
                splitpane.items.removeIf { it.id == "postinfo_panel" }
            }
        }
        widgetsController.currentSettings.visibleToolbarPanelProperty.onChange { it ->
            if (it) {
                root.children.add(1, find<ActionBarFragment>().root)
            } else {
                root.children.removeIf { it.id == "action_toolbar" }
            }
        }
        widgetsController.currentSettings.visibleStatusBarPanelProperty.onChange { it ->
            if (it) {
                root.children.add(find<StatusBarFragment>().root)
            } else {
                root.children.removeIf { it.id == "statusbar" }
            }
        }
        widgetsController.currentSettings.darkModeProperty.onChange {
            if (it) {
                Application.setUserAgentStylesheet(CupertinoDark().userAgentStylesheet)
            } else {
                Application.setUserAgentStylesheet(CupertinoLight().userAgentStylesheet)
            }
        }
    }

    private fun buildInfoPanel(it: PostModel?): PostInfoPanelFragment {
        val postInfoPanelFragment = find<PostInfoPanelFragment>()
        if (it != null) {
            postInfoPanelFragment.setPostId(it.postId)
        }
        postInfoPanelFragment.root.selectionModel.select(lastSelectedInfoPanelTab)
        postInfoPanelFragment.root.selectionModel.selectedIndexProperty().onChange {
            lastSelectedInfoPanelTab = it
        }
        return postInfoPanelFragment
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}