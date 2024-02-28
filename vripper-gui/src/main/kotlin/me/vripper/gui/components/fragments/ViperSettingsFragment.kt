package me.vripper.gui.components.fragments

import atlantafx.base.controls.ToggleSwitch
import javafx.collections.FXCollections
import me.vripper.gui.controller.SettingsController
import me.vripper.gui.model.settings.ViperSettingsModel
import tornadofx.*

class ViperSettingsFragment : Fragment("Viper Settings") {
    private val settingsController: SettingsController by inject()
    private val proxies = FXCollections.observableArrayList<String>()
    private val viperGirlsSettings = settingsController.findViperGirlsSettings()
    val viperSettingsModel = ViperSettingsModel()

    override fun onDock() {
        proxies.clear()
        proxies.addAll(settingsController.getProxies())
        viperSettingsModel.username = viperGirlsSettings.username
        viperSettingsModel.password = viperGirlsSettings.password
        viperSettingsModel.thanks = viperGirlsSettings.thanks
        viperSettingsModel.host = viperGirlsSettings.host
    }

    override val root = vbox {
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