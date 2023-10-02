package me.mnlr.vripper.gui.view.settings

import javafx.collections.FXCollections
import me.mnlr.vripper.gui.controller.SettingsController
import me.mnlr.vripper.gui.model.settings.ViperSettingsModel
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
                    field("Select a proxy") {
                        combobox(viperSettingsModel.hostProperty, proxies)
                    }
                }
            }
        }
    }
}