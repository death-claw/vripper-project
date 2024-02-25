package me.vripper.gui.components.views

import javafx.scene.control.ContentDisplay
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import me.vripper.gui.Styles
import me.vripper.gui.components.fragments.SettingsFragment
import tornadofx.*

class ActionBarView : View() {

    override val root = borderpane {
        padding = insets(all = 5)
        left<AddActionsView>()
        center<DownloadActionsView>()
        right {
            button("Settings") {
                imageview("settings.png") {
                    fitWidth = 32.0
                    fitHeight = 32.0
                }
                addClass(Styles.actionBarButton)
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                tooltip("Open settings menu [S]")
                shortcut(KeyCodeCombination(KeyCode.S))
                action {
                    find<SettingsFragment>().openModal()?.apply {
                        minWidth = 600.0
                        minHeight = 400.0
                    }
                }
            }
        }
    }
}