package me.vripper.gui.components.fragments

import atlantafx.base.controls.ToggleSwitch
import javafx.collections.FXCollections
import kotlinx.coroutines.*
import me.vripper.gui.controller.SettingsController
import me.vripper.gui.model.settings.ViperSettingsModel
import me.vripper.model.ViperSettings
import tornadofx.*

class ViperSettingsFragment : Fragment("Viper Settings") {
    private val settingsController: SettingsController by inject()
    private val proxies = FXCollections.observableArrayList<String>()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private lateinit var viperGirlsSettings: ViperSettings
    val viperSettingsModel = ViperSettingsModel()

    override val root = vbox {}

    init {
        coroutineScope.launch {
            async {
                viperGirlsSettings = settingsController.findViperGirlsSettings()
                viperSettingsModel.username = viperGirlsSettings.username
                viperSettingsModel.password = viperGirlsSettings.password
                viperSettingsModel.thanks = viperGirlsSettings.thanks
                viperSettingsModel.host = viperGirlsSettings.host
                proxies.addAll(settingsController.getProxies())
            }.await()
            runLater {
                with(root) {
                    form {
                        fieldset {
                            field("Select a proxy") {
                                combobox(viperSettingsModel.hostProperty, proxies)
                            }
                            field("Enable ViperGirls Authentication") {
                                add(ToggleSwitch().apply {
                                    isSelected = viperGirlsSettings.login
                                    viperSettingsModel.loginProperty.bind(selectedProperty())
                                })
                            }
                            fieldset {
                                visibleWhen(viperSettingsModel.loginProperty)
                                field("ViperGirls Username") {
                                    textfield(viperSettingsModel.usernameProperty)
                                }
                                field("ViperGirls Password") {
                                    passwordfield(viperSettingsModel.passwordProperty)
                                }
                                field("Leave like") {
                                    checkbox {
                                        bind(viperSettingsModel.thanksProperty)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}