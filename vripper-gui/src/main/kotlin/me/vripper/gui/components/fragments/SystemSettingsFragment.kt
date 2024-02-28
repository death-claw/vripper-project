package me.vripper.gui.components.fragments

import atlantafx.base.controls.ToggleSwitch
import atlantafx.base.util.IntegerStringConverter
import javafx.scene.control.Spinner
import me.vripper.gui.controller.SettingsController
import me.vripper.gui.model.settings.SystemSettingsModel
import tornadofx.*

class SystemSettingsFragment : Fragment("System Settings") {
    private val settingsController: SettingsController by inject()
    private val systemSettings = settingsController.findSystemSettings()
    val systemSettingsModel = SystemSettingsModel()

    override fun onDock() {
        systemSettingsModel.tempPath = systemSettings.tempPath
        systemSettingsModel.cachePath = systemSettings.cachePath
    }

    override val root = vbox {
        form {
            fieldset {
                field("Temporary Path") {
                    textfield(systemSettingsModel.tempPathProperty) {
                        isEditable = false
                    }
                    button("Browse") {
                        action {
                            val directory = chooseDirectory(title = "Select temporary folder")
                            if (directory != null) {
                                systemSettingsModel.tempPathProperty.set(directory.path)
                            }
                        }
                    }
                }
                field("Cache Path") {
                    textfield(systemSettingsModel.cachePathProperty) {
                        isEditable = false
                    }
                    button("Browse") {
                        action {
                            val directory = chooseDirectory(title = "Select temporary folder")
                            if (directory != null) {
                                systemSettingsModel.cachePathProperty.set(directory.path)
                            }
                        }
                    }
                }
                field("Max log entries") {
                    add(Spinner<Int>(10, 10000, systemSettings.maxEventLog).apply {
                        systemSettingsModel.logEntriesProperty.bind(valueProperty())
                        isEditable = true
                        IntegerStringConverter.createFor(this)
                    })
                }
                fieldset {
                    field("Clipboard monitoring") {
                        add(ToggleSwitch().apply {
                            isSelected = systemSettings.enableClipboardMonitoring
                            systemSettingsModel.enableProperty.bind(selectedProperty())
                        })
                    }
                    fieldset {
                        visibleWhen(systemSettingsModel.enableProperty)
                        field("Polling rate (ms)") {
                            add(Spinner<Int>(500, Int.MAX_VALUE, systemSettings.clipboardPollingRate).apply {
                                systemSettingsModel.pollingRateProperty.bind(valueProperty())
                                isEditable = true
                                IntegerStringConverter.createFor(this)
                            })
                        }
                    }
                }
            }
        }
    }
}