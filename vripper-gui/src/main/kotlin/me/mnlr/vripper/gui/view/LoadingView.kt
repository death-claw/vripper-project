package me.mnlr.vripper.gui.view

import javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS
import javafx.scene.effect.DropShadow
import javafx.util.Duration
import me.mnlr.vripper.gui.event.ApplicationInitialized
import tornadofx.*

class LoadingView : View("VRipper") {

    private val appView: AppView by inject()

    init {
        subscribe<ApplicationInitialized> {
            runLater {
                replaceWith(appView, ViewTransition.FadeThrough(Duration.millis(250.0)))
            }
        }
    }

    override val root = borderpane {
        padding = insets(all = 5)
        center {
            progressindicator {
                progress = INDETERMINATE_PROGRESS
            }
        }
        effect = DropShadow()
    }
}