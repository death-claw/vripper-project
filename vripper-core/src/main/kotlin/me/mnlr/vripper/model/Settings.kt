package me.mnlr.vripper.model

class Settings {
    var desktopClipboard: Boolean = false
    var maxEventLog: Int = 1_000
    var connectionSettings: ConnectionSettings = ConnectionSettings()
    var downloadSettings: DownloadSettings = DownloadSettings()
    var viperSettings: ViperSettings = ViperSettings()
}

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
    var forumSubfolder: Boolean = false,
    var threadSubLocation: Boolean = false,
    var clearCompleted: Boolean = false,
    var appendPostId: Boolean = false
)

data class ConnectionSettings(
    var maxThreads: Int = 4,
    var maxTotalThreads: Int = 0,
    var timeout: Int = 30,
    var maxAttempts: Int = 3,
)