package me.mnlr.vripper.gui.view

import me.mnlr.vripper.gui.controller.MainController
import me.mnlr.vripper.gui.view.actionbar.ActionBarView
import me.mnlr.vripper.gui.view.main.MainView
import me.mnlr.vripper.gui.view.status.StatusBarView
import tornadofx.*

class AppView : View() {

    private val mainController: MainController by inject()

    init {
        title = "VRipper ${mainController.version}"
    }

    override val root = borderpane {
        top<ActionBarView>()
        center<MainView>()
        bottom<StatusBarView>()
    }
}