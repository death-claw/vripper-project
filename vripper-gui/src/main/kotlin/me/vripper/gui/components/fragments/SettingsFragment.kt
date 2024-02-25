package me.vripper.gui.components.fragments

import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.TabPane
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import me.vripper.exception.ValidationException
import me.vripper.gui.controller.SettingsController
import tornadofx.*


class SettingsFragment : Fragment("Settings") {

    private val settingsController: SettingsController by inject()

    private val downloadSettingsFragment: DownloadSettingsFragment = find()
    private val connectionSettingsFragment: ConnectionSettingsFragment = find()
    private val viperSettingsFragment: ViperSettingsFragment = find()
    private val systemSettingsFragment: SystemSettingsFragment = find()

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        spacing = 5.0
        tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            VBox.setVgrow(this, Priority.ALWAYS)
            tab(downloadSettingsFragment.title) {
                add(downloadSettingsFragment)
                val imageView = ImageView("downloads-folder.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
            tab(connectionSettingsFragment.title) {
                add(connectionSettingsFragment)
                val imageView = ImageView("data-transfer.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
            tab(viperSettingsFragment.title) {
                add(viperSettingsFragment)
                val imageView = ImageView("icons/32x32.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
            tab(systemSettingsFragment.title) {
                add(systemSettingsFragment)
                val imageView = ImageView("clipboard.png")
                imageView.fitWidth = 18.0
                imageView.fitHeight = 18.0
                graphic = imageView
            }
        }
        borderpane {
            right {
                padding = insets(all = 5.0)
                button("Save") {
                    imageview("save.png") {
                        fitWidth = 18.0
                        fitHeight = 18.0
                    }
                    action {
                        try {
                            settingsController.saveNewSettings(
                                downloadSettingsFragment.downloadSettingsModel,
                                connectionSettingsFragment.connectionSettingsModel,
                                viperSettingsFragment.viperSettingsModel,
                                systemSettingsFragment.systemSettingsModel
                            )
                            close()
                        } catch (e: ValidationException) {
                            alert(Alert.AlertType.ERROR, "Invalid settings", e.message)
                        }

                    }
                }
            }
        }
    }
}