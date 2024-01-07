package me.vripper.gui.view.settings

import io.github.palexdev.materialfx.controls.MFXButton
import io.github.palexdev.materialfx.enums.ButtonType
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.TabPane
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import me.vripper.exception.ValidationException
import me.vripper.gui.controller.SettingsController
import tornadofx.*


class SettingsView : Fragment("Settings") {

    private val settingsController: SettingsController by inject()

    private val downloadSettingsView: DownloadSettingsView by inject()
    private val connectionSettingsView: ConnectionSettingsView by inject()
    private val viperSettingsView: ViperSettingsView by inject()
    private val systemSettingsView: SystemSettingsView by inject()

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        spacing = 5.0
        tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            VBox.setVgrow(this, Priority.ALWAYS)
            tab<DownloadSettingsView> {
                val imageView = ImageView("downloads-folder.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
            tab<ConnectionSettingsView> {
                val imageView = ImageView("data-transfer.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
            tab<ViperSettingsView> {
                val imageView = ImageView("icons/32x32.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
            tab<SystemSettingsView> {
                val imageView = ImageView("clipboard.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
        }
        borderpane {
            right {
                padding = insets(all = 5.0)
                MFXButton("Save").apply {
                    buttonType = ButtonType.RAISED
                    imageview("save.png") {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                    action {
                        try {
                            settingsController.saveNewSettings(
                                downloadSettingsView.downloadSettingsModel,
                                connectionSettingsView.connectionSettingsModel,
                                viperSettingsView.viperSettingsModel,
                                systemSettingsView.systemSettingsModel
                            )
                            close()
                        } catch (e: ValidationException) {
                            alert(Alert.AlertType.ERROR, "Invalid settings", e.message)
                        }
                    }
                }.attachTo(this)
            }
        }
    }
}