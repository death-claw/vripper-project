package me.vripper.gui.components.fragments

import atlantafx.base.util.IntegerStringConverter
import javafx.scene.control.Spinner
import me.vripper.gui.controller.SettingsController
import me.vripper.gui.model.settings.ConnectionSettingsModel
import tornadofx.*

class ConnectionSettingsFragment : Fragment("Connection Settings") {
    private val settingsController: SettingsController by inject()
    private val connectionSettings = settingsController.findConnectionSettings()
    val connectionSettingsModel = ConnectionSettingsModel()

    override fun onDock() {
    }

    override val root = vbox {
        form {
            fieldset {
                field("Concurrent downloads per host") {
                    add(Spinner<Int>(1, 4, connectionSettings.maxConcurrentPerHost).apply {
                        connectionSettingsModel.maxThreadsProperty.bind(valueProperty())
                        isEditable = true
                        IntegerStringConverter.createFor(this)
                    })
                }
                field("Global concurrent downloads") {
                    add(Spinner<Int>(0, 24, connectionSettings.maxGlobalConcurrent).apply {
                        connectionSettingsModel.maxTotalThreadsProperty.bind(valueProperty())
                        isEditable = true
                        IntegerStringConverter.createFor(this)
                    })
                }
                field("Connection timeout (s)") {
                    add(Spinner<Int>(1, 300, connectionSettings.timeout.toInt()).apply {
                        connectionSettingsModel.timeoutProperty.bind(valueProperty())
                        isEditable = true
                        IntegerStringConverter.createFor(this)
                    })
                }
                field("Maximum attempts") {
                    add(Spinner<Int>(1, 10, connectionSettings.maxAttempts).apply {
                        connectionSettingsModel.maxAttemptsProperty.bind(valueProperty())
                        isEditable = true
                        IntegerStringConverter.createFor(this)
                    })
                }
            }
        }
    }
}