package me.vripper.gui.view.settings

import javafx.collections.FXCollections
import me.vripper.gui.controller.SettingsController
import me.vripper.gui.model.settings.ViperSettingsModel
import tornadofx.*

class ViperSettingsView : View("Viper Settings") {
    private val settingsController: SettingsController by inject()
    val viperSettingsModel = ViperSettingsModel()
    val proxies = FXCollections.observableArrayList<String>()

    override fun onDock() {
        proxies.clear()
        proxies.addAll(settingsController.getProxies())
        val viperGirlsSettings = settingsController.findViperGirlsSettings()
        viperSettingsModel.login = viperGirlsSettings.login
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
                    checkbox {
                        bind(viperSettingsModel.loginProperty)
                    }
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