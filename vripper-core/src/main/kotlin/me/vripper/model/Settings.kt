package me.vripper.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val connectionSettings: ConnectionSettings = ConnectionSettings(),
    val downloadSettings: DownloadSettings = DownloadSettings(),
    val viperSettings: ViperSettings = ViperSettings(),
    val systemSettings: SystemSettings = SystemSettings()
)

@Serializable
data class ViperSettings(
    val login: Boolean = false,
    val username: String = "",
    val password: String = "",
    val thanks: Boolean = false,
    val host: String = "https://vipergirls.to",
)

@Serializable
data class DownloadSettings(
    val downloadPath: String = System.getProperty("user.home"),
    val autoStart: Boolean = true,
    val autoQueueThreshold: Int = 1,
    val forceOrder: Boolean = false,
    val forumSubDirectory: Boolean = false,
    val threadSubLocation: Boolean = false,
    val clearCompleted: Boolean = false,
    val appendPostId: Boolean = false
)

@Serializable
data class ConnectionSettings(
    val maxConcurrentPerHost: Int = 2,
    val maxGlobalConcurrent: Int = 0,
    val timeout: Int = 30,
    val maxAttempts: Int = 3,
)

@Serializable
data class SystemSettings(
    val tempPath: String = System.getProperty("java.io.tmpdir"),
    val enableClipboardMonitoring: Boolean = false,
    val clipboardPollingRate: Int = 500,
    val maxEventLog: Int = 1_000,
)
