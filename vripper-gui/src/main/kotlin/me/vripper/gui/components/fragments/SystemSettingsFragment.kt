package me.vripper.gui.components.fragments

import me.vripper.gui.controller.SettingsController
import me.vripper.gui.model.settings.SystemSettingsModel
import tornadofx.*

class SystemSettingsFragment : Fragment("System Settings") {
    private val settingsController: SettingsController by inject()
    val systemSettingsModel = SystemSettingsModel()

    override fun onDock() {
        val systemSettings = settingsController.findSystemSettings()
        systemSettingsModel.tempPath = systemSettings.tempPath
        systemSettingsModel.cachePath = systemSettings.cachePath
        systemSettingsModel.enable = systemSettings.enableClipboardMonitoring
        systemSettingsModel.pollingRate = systemSettings.clipboardPollingRate.toString()
        systemSettingsModel.logEntries = systemSettings.maxEventLog.toString()
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
                    textfield(systemSettingsModel.logEntriesProperty) {
                        filterInput { it.controlNewText.isInt() }
                    }
                }
                fieldset {
                    field("Clipboard monitoring") {
                        checkbox {
                            bind(systemSettingsModel.enableProperty)
                        }
                    }
                    fieldset {
                        visibleWhen(systemSettingsModel.enableProperty)
                        field("Polling rate (ms)") {
                            textfield(systemSettingsModel.pollingRateProperty) {
                                filterInput {
                                    it.controlNewText.isInt()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}