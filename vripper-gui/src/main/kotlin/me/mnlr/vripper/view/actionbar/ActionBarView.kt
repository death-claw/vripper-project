package me.mnlr.vripper.view.actionbar

import javafx.scene.control.ContentDisplay
import me.mnlr.vripper.gui.Styles
import me.mnlr.vripper.view.settings.SettingsView
import tornadofx.*

class ActionBarView : View("Vripper") {

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
                    find<SettingsView>().openModal()
                }
            }
        }
    }
}