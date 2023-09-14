package me.mnlr.vripper.model

data class Settings(
    var maxEventLog: Int = 1_000,
    var connectionSettings: ConnectionSettings = ConnectionSettings(),
    var downloadSettings: DownloadSettings = DownloadSettings(),
    var viperSettings: ViperSettings = ViperSettings(),
    var clipboardSettings: ClipboardSettings = ClipboardSettings()
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
    var tempPath: String = System.getProperty("java.io.tmpdir"),
    var autoStart: Boolean = true,
    var autoQueueThreshold: Int = 1,
    var forceOrder: Boolean = false,
    var forumSubfolder: Boolean = false,
    var threadSubLocation: Boolean = false,
    var clearCompleted: Boolean = false,
    var appendPostId: Boolean = false
)

data class ConnectionSettings(
    var maxThreads: Int = 2,
    var maxTotalThreads: Int = 0,
    var timeout: Int = 30,
    var maxAttempts: Int = 3,
)

data class ClipboardSettings(var enable: Boolean = false, var pollingRate: Int = 500)