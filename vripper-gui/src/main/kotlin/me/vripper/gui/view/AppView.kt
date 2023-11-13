package me.vripper.gui.view

import me.vripper.gui.controller.MainController
import me.vripper.gui.view.actionbar.ActionBarView
import me.vripper.gui.view.main.MainView
import me.vripper.gui.view.status.StatusBarView
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