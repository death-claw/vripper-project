package me.mnlr.vripper.view.settings

import me.mnlr.vripper.controller.SettingsController
import me.mnlr.vripper.model.settings.DownloadSettingsModel
import tornadofx.*


class DownloadSettingsView : View("Download Settings") {

    private val settingsController: SettingsController by inject()
    val downloadSettingsModel = DownloadSettingsModel()

    override fun onDock() {
        val downloadSettings = settingsController.findDownloadSettings()
        downloadSettingsModel.downloadPath = downloadSettings.downloadPath
        downloadSettingsModel.autoStart = downloadSettings.autoStart
        downloadSettingsModel.autoQueueThreshold = downloadSettings.autoQueueThreshold
        downloadSettingsModel.forceOrder = downloadSettings.forceOrder
        downloadSettingsModel.forumSubfolder = downloadSettings.forumSubfolder
        downloadSettingsModel.threadSubLocation = downloadSettings.threadSubLocation
        downloadSettingsModel.clearCompleted = downloadSettings.clearCompleted
        downloadSettingsModel.appendPostId = downloadSettings.appendPostId
    }

    override val root = vbox {
        form {
            fieldset {
                field("Download Path") {
                    textfield(downloadSettingsModel.downloadPathProperty) {
                        isEditable = false
                    }
                    button("Browse") {
                        action {
                            val directory = chooseDirectory(title = "Select download folder")
                            if(directory != null) {
                                downloadSettingsModel.downloadPathProperty.set(directory.path)
                            }
                        }
                    }
                }
                field("Auto start downloads") {
                    checkbox {
                        bind(downloadSettingsModel.autoStartProperty)
                    }
                }
                field("Auto queue thread if post count is below or equal to") {
                    textfield(downloadSettingsModel.autoQueueThresholdProperty) {
                        filterInput { it.controlNewText.isInt() }
                    }
                }
                field("Organize by category") {
                    checkbox {
                        bind(downloadSettingsModel.forumSubfolderProperty)
                    }
                }
                field("Organize by thread") {
                    checkbox {
                        bind(downloadSettingsModel.threadSubLocationProperty)
                    }
                }
                field("Order images") {
                    checkbox {
                        bind(downloadSettingsModel.forceOrderProperty)
                    }
                }
                field("Append post id to download folder") {
                    checkbox {
                        bind(downloadSettingsModel.appendPostIdProperty)
                    }
                }
                field("Clear Finished") {
                    checkbox {
                        bind(downloadSettingsModel.clearCompletedProperty)
                    }
                }
            }
        }
    }
}