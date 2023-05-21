package me.mnlr.vripper.controller

import me.mnlr.vripper.model.*
import me.mnlr.vripper.model.settings.ClipboardSettingsModel
import me.mnlr.vripper.model.settings.ConnectionSettingsModel
import me.mnlr.vripper.model.settings.DownloadSettingsModel
import me.mnlr.vripper.model.settings.ViperSettingsModel
import me.mnlr.vripper.services.SettingsService
import tornadofx.*

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

    fun findClipboardSettings(): ClipboardSettings {
        return settingsService.settings.clipboardSettings
    }

    fun saveNewSettings(
        downloadSettingsModel: DownloadSettingsModel,
        connectionSettingsModel: ConnectionSettingsModel,
        viperSettingsModel: ViperSettingsModel,
        clipboardSettingsModel: ClipboardSettingsModel
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
            clipboardSettings =
                ClipboardSettings(
                    clipboardSettingsModel.enable,
                    if (clipboardSettingsModel.pollingRate.isBlank()) 500 else clipboardSettingsModel.pollingRate.toInt()
                )
        })

    }

    fun getProxies(): List<String> {
        return settingsService.getProxies()
    }
}