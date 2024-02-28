package me.vripper.gui.controller

import me.vripper.gui.model.settings.ConnectionSettingsModel
import me.vripper.gui.model.settings.DownloadSettingsModel
import me.vripper.gui.model.settings.SystemSettingsModel
import me.vripper.gui.model.settings.ViperSettingsModel
import me.vripper.model.*
import me.vripper.services.SettingsService
import tornadofx.Controller

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

    fun findSystemSettings(): SystemSettings {
        return settingsService.settings.systemSettings
    }

    fun saveNewSettings(
        downloadSettingsModel: DownloadSettingsModel,
        connectionSettingsModel: ConnectionSettingsModel,
        viperSettingsModel: ViperSettingsModel,
        systemSettingsModel: SystemSettingsModel
    ) {
        settingsService.newSettings(
            Settings(
                downloadSettings = DownloadSettings(
                    downloadSettingsModel.downloadPath,
                    downloadSettingsModel.autoStart,
                    downloadSettingsModel.autoQueueThreshold,
                    downloadSettingsModel.forceOrder,
                    downloadSettingsModel.forumSubfolder,
                    downloadSettingsModel.threadSubLocation,
                    downloadSettingsModel.clearCompleted,
                    downloadSettingsModel.appendPostId
                ),
                connectionSettings = ConnectionSettings(
                    connectionSettingsModel.maxThreads,
                    connectionSettingsModel.maxTotalThreads,
                    connectionSettingsModel.timeout,
                    connectionSettingsModel.maxAttempts,
                ),
                viperSettings = ViperSettings(
                    viperSettingsModel.login,
                    viperSettingsModel.username,
                    viperSettingsModel.password,
                    viperSettingsModel.thanks,
                    viperSettingsModel.host,
                ),
                systemSettings =
                SystemSettings(
                    systemSettingsModel.tempPath,
                    systemSettingsModel.cachePath,
                    systemSettingsModel.enable,
                    systemSettingsModel.pollingRate,
                    systemSettingsModel.logEntries
                )
            )
        )
    }

    fun getProxies(): List<String> {
        return settingsService.getProxies()
    }
}