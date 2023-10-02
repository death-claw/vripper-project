package me.mnlr.vripper.gui.view.actionbar

import javafx.geometry.Orientation
import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import me.mnlr.vripper.gui.Styles
import me.mnlr.vripper.gui.view.posts.AddView
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
            tooltip("Add new links [Ctrl+L]")
            shortcut(KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN))
            action {
                find<AddView>().input.clear()
                find<AddView>().openModal()
            }
        }
        separator(Orientation.VERTICAL)
    }
}