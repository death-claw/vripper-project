package me.mnlr.vripper.view.settings

import me.mnlr.vripper.controller.SettingsController
import me.mnlr.vripper.model.settings.ClipboardSettingsModel
import tornadofx.*

class ClipboardSettingsView : View("Clipboard Settings") {
    private val settingsController: SettingsController by inject()
    val clipboardSettingsModel = ClipboardSettingsModel()

    override fun onDock() {
        val clipboardSettings = settingsController.findClipboardSettings()
        clipboardSettingsModel.enable = clipboardSettings.enable
        clipboardSettingsModel.pollingRate = clipboardSettings.pollingRate.toString()
    }

    override val root = vbox {
        form {
            fieldset {
                field("Enable") {
                    checkbox {
                        bind(clipboardSettingsModel.enableProperty)
                    }
                }
                fieldset {
                    visibleWhen(clipboardSettingsModel.enableProperty)
                    field("Polling rate (ms)") {
                        textfield(clipboardSettingsModel.pollingRateProperty) {
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