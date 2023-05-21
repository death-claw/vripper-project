package me.mnlr.vripper.view.settings

import me.mnlr.vripper.controller.SettingsController
import me.mnlr.vripper.model.settings.ConnectionSettingsModel
import tornadofx.*

class ConnectionSettingsView : View("Connection settings") {
    private val settingsController: SettingsController by inject()
    val connectionSettingsModel = ConnectionSettingsModel()

    override fun onDock() {
        val connectionSettings = settingsController.findConnectionSettings()
        connectionSettingsModel.maxThreads = connectionSettings.maxThreads
        connectionSettingsModel.maxTotalThreads = connectionSettings.maxTotalThreads
        connectionSettingsModel.timeout = connectionSettings.timeout
        connectionSettingsModel.maxAttempts = connectionSettings.maxAttempts
    }

    override val root = vbox {
        form {
            fieldset {
                field("Concurrent downloads per host") {
                    textfield(connectionSettingsModel.maxThreadsProperty) {
                        filterInput { it.controlNewText.isInt() }
                    }
                }
                field("Global concurrent downloads") {
                    textfield(connectionSettingsModel.maxTotalThreadsProperty) {
                        filterInput { it.controlNewText.isInt() }
                    }
                }
                field("Connection timeout (s)") {
                    textfield(connectionSettingsModel.timeoutProperty) {
                        filterInput { it.controlNewText.isInt() }
                    }
                }
                field("Maximum attempts") {
                    textfield(connectionSettingsModel.maxAttemptsProperty) {
                        filterInput { it.controlNewText.isInt() }
                    }
                }
            }
        }
    }
}