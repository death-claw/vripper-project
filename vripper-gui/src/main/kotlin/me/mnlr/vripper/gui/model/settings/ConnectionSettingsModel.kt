package me.mnlr.vripper.gui.model.settings

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import tornadofx.*

class ConnectionSettingsModel {
    val maxThreadsProperty = SimpleIntegerProperty()
    var maxThreads: Int by maxThreadsProperty

    val maxTotalThreadsProperty = SimpleIntegerProperty()
    var maxTotalThreads: Int by maxTotalThreadsProperty

    val timeoutProperty = SimpleLongProperty()
    var timeout: Long by timeoutProperty

    val maxAttemptsProperty = SimpleIntegerProperty()
    var maxAttempts: Int by maxAttemptsProperty
}
