package me.vripper.model

data class Settings(
    var connectionSettings: ConnectionSettings = ConnectionSettings(),
    var downloadSettings: DownloadSettings = DownloadSettings(),
    var viperSettings: ViperSettings = ViperSettings(),
    var systemSettings: SystemSettings = SystemSettings()
)

data class ViperSettings(
    var login: Boolean = false,
    var username: String = "",
    var password: String = "",
    var thanks: Boolean = false,
    var host: String = "https://vipergirls.to",
)

data class DownloadSettings(
    var downloadPath: String = System.getProperty("user.home"),
    var autoStart: Boolean = true,
    var autoQueueThreshold: Int = 1,
    var forceOrder: Boolean = false,
    var forumSubDirectory: Boolean = false,
    var threadSubLocation: Boolean = false,
    var clearCompleted: Boolean = false,
    var appendPostId: Boolean = false
)

data class ConnectionSettings(
    var maxConcurrentPerHost: Int = 2,
    var maxGlobalConcurrent: Int = 0,
    var timeout: Long = 30,
    var maxAttempts: Int = 3,
)

data class SystemSettings(
    var tempPath: String = System.getProperty("java.io.tmpdir"),
    var cachePath: String = System.getProperty("java.io.tmpdir"),
    var enableClipboardMonitoring: Boolean = false,
    var clipboardPollingRate: Int = 500,
    var maxEventLog: Int = 1_000,
)
