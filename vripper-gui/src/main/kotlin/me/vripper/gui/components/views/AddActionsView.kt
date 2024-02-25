package me.vripper.gui.components.views

import javafx.geometry.Orientation
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import me.vripper.gui.Styles
import me.vripper.gui.components.fragments.AddLinksFragment
import tornadofx.*

class AddActionsView : View() {

    override val root = hbox {
        button("Add links") {
            imageview("plus.png") {
                fitWidth = 32.0
                fitHeight = 32.0
            }
            addClass(Styles.actionBarButton)
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
    }
}