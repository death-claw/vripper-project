package me.mnlr.vripper.view

import me.mnlr.vripper.controller.MainController
import me.mnlr.vripper.view.actionbar.ActionBarView
import me.mnlr.vripper.view.main.MainView
import me.mnlr.vripper.view.status.StatusBarView
import tornadofx.View
import tornadofx.borderpane

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