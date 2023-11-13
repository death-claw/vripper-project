package me.vripper.gui.view.actionbar

import javafx.scene.control.ContentDisplay
import me.vripper.gui.Styles
import me.vripper.gui.view.settings.SettingsView
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
                tooltip("Open settings menu")
                action {
                    find<SettingsView>().openModal()?.apply {
                        minWidth = 600.0
                        minHeight = 400.0
                    }
                }
            }
        }
    }
}