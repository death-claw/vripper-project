package me.mnlr.vripper.controller

import javafx.scene.control.Alert
import me.mnlr.vripper.exception.ValidationException
import me.mnlr.vripper.model.ConnectionSettings
import me.mnlr.vripper.model.DownloadSettings
import me.mnlr.vripper.model.settings.DownloadSettingsModel
import me.mnlr.vripper.model.Settings
import me.mnlr.vripper.model.ViperSettings
import me.mnlr.vripper.model.settings.ConnectionSettingsModel
import me.mnlr.vripper.model.settings.ViperSettingsModel
import me.mnlr.vripper.services.SettingsService
import tornadofx.Controller
import tornadofx.alert
import tornadofx.warning

class SettingsController : Controller() {

    private val settingsService: SettingsService by di()

    fun findDownloadSettings(): DownloadSettings {
        return settingsService.settings.downloadSettings
    }

    fun findConnectionSettings(): ConnectionSettings {
        return settingsService.settings.connectionSettings
    }

    fun findViperGirlsSettings(): ViperSettings {
        return settingsService.settings.viperSettings
    }

    fun saveNewSettings(
        downloadSettingsModel: DownloadSettingsModel,
        connectionSettingsModel: ConnectionSettingsModel,
        viperSettingsModel: ViperSettingsModel
    ) {
            settingsService.newSettings(Settings().apply {
                downloadSettings = DownloadSettings(
                    downloadSettingsModel.downloadPath,
                    downloadSettingsModel.autoStart,
                    downloadSettingsModel.autoQueueThreshold,
                    downloadSettingsModel.forceOrder,
                    downloadSettingsModel.forumSubfolder,
                    downloadSettingsModel.threadSubLocation,
                    downloadSettingsModel.clearCompleted,
                    downloadSettingsModel.appendPostId
                )
                connectionSettings = ConnectionSettings(
                    connectionSettingsModel.maxThreads,
                    connectionSettingsModel.maxTotalThreads,
                    connectionSettingsModel.timeout,
                    connectionSettingsModel.maxAttempts,
                )
                viperSettings = ViperSettings(
                    viperSettingsModel.login,
                    viperSettingsModel.username,
                    viperSettingsModel.password,
                    viperSettingsModel.thanks,
                    viperSettingsModel.host,
                )
            })

    }

    fun getProxies(): List<String> {
        return settingsService.getProxies()
    }
}