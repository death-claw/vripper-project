package me.vripper.gui.model.settings

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class SystemSettingsModel {
    val tempPathProperty = SimpleStringProperty()
    var tempPath: String by tempPathProperty

    val cachePathProperty = SimpleStringProperty()
    var cachePath: String by cachePathProperty

    val logEntriesProperty = SimpleStringProperty()
    var logEntries: String by logEntriesProperty

    val enableProperty = SimpleBooleanProperty()
    var enable: Boolean by enableProperty

    val pollingRateProperty = SimpleStringProperty()
    var pollingRate: String by pollingRateProperty
}
