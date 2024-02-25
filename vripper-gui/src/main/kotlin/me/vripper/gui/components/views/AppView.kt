package me.vripper.gui.components.views

import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.*
import me.vripper.gui.components.fragments.PostInfoPanelFragment
import me.vripper.gui.controller.MainController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.model.PostModel
import tornadofx.*

class AppView : View() {

    override val root = VBox()
    private val mainController: MainController by inject()
    private val postsTableView: PostsTableView by inject()
    private val menuBarView: MenuBarView by inject()
    private val actionBarView: ActionBarView by inject()
    private val statusBarView: StatusBarView by inject()
    private val mainView: MainView by inject()
    private val widgetsController: WidgetsController by inject()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var infoPanelView = HBox()
    private var splitpane: SplitPane
    private var lastSelectedInfoPanelTab = 0

    init {
        title = "VRipper ${mainController.version}"
        setPostIdToInfoPanelView(null)
        with(root) {
            add(menuBarView)
            if (widgetsController.currentSettings.visibleToolbarPanel) {
                add(actionBarView)
            }
            splitpane = splitpane {
                add(mainView)
                if (widgetsController.currentSettings.visibleInfoPanel) {
                    add(infoPanelView)
                    setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
                }
                orientation = Orientation.VERTICAL
                vboxConstraints { vGrow = Priority.ALWAYS }
            }
            if (widgetsController.currentSettings.visibleStatusBarPanel) {
                add(statusBarView)
            }
            prefWidth = widgetsController.currentSettings.width
            prefHeight = widgetsController.currentSettings.height
        }
    }

    override fun onDock() {
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
            setPostIdToInfoPanelView(it)
        }
        mainView.root.selectionModel.selectedIndexProperty().onChange {
            if (it == 0 && widgetsController.currentSettings.visibleInfoPanel) {
                val selectedItem = postsTableView.tableView.selectedItem
                setPostIdToInfoPanelView(selectedItem)
                splitpane.add(infoPanelView)
                splitpane.setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
            } else {
                splitpane.items.remove(infoPanelView)
                infoPanelView.clear()
            }
        }
        widgetsController.currentSettings.visibleInfoPanelProperty.onChange {
            if (mainView.root.selectionModel.selectedIndex != 0) return@onChange
            if (it) {
                val selectedItem = postsTableView.tableView.selectedItem
                setPostIdToInfoPanelView(selectedItem)
                splitpane.add(infoPanelView)
                splitpane.setDividerPositions(widgetsController.currentSettings.infoPanelDividerPosition)
            } else {
                splitpane.items.remove(infoPanelView)
                infoPanelView.clear()
            }
        }
        widgetsController.currentSettings.visibleToolbarPanelProperty.onChange {
            if (it) {
                root.children.add(1, actionBarView.root)
            } else {
                root.children.remove(actionBarView.root)
            }
        }
        widgetsController.currentSettings.visibleStatusBarPanelProperty.onChange {
            if (it) {
                root.children.add(statusBarView.root)
            } else {
                root.children.remove(statusBarView.root)
            }
        }
    }

    private fun setPostIdToInfoPanelView(it: PostModel?) {
        if (!widgetsController.currentSettings.visibleInfoPanel) {
            return
        }
        val postInfoPanelFragment = find<PostInfoPanelFragment>()
        if (it != null) {
            postInfoPanelFragment.setPostId(it.postId)
        }
        postInfoPanelFragment.root.hgrow = Priority.ALWAYS
        postInfoPanelFragment.root.selectionModel.select(lastSelectedInfoPanelTab)
        postInfoPanelFragment.root.selectionModel.selectedIndexProperty().onChange {
            lastSelectedInfoPanelTab = it
        }
        infoPanelView.clear()
        infoPanelView.add(postInfoPanelFragment)
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}