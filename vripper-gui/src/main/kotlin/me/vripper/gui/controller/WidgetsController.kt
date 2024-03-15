package me.vripper.gui.controller

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.vripper.gui.model.WidgetsViewModel
import me.vripper.utilities.ApplicationProperties
import tornadofx.Controller
import tornadofx.onChange
import java.nio.file.Files

private val WIDGETS_CONFIG_PATH = ApplicationProperties.VRIPPER_DIR.resolve("widgets-config.json")

class WidgetsController : Controller() {

    private val json = Json {
        prettyPrint = true
        allowSpecialFloatingPointValues = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    data class ThreadSelectionTableColumns(
        val preview: Boolean = true,
        val index: Boolean = true,
        val title: Boolean = true,
        val link: Boolean = true,
        val hosts: Boolean = true,
    )

    @Serializable
    data class ThreadSelectionTableColumnsWidth(
        val preview: Double = 100.0,
        val index: Double = 100.0,
        val title: Double = 200.0,
        val link: Double = 200.0,
        val hosts: Double = 100.0,
    )

    @Serializable
    data class LogsTableColumns(
        val time: Boolean = true,
        val type: Boolean = true,
        val status: Boolean = true,
        val message: Boolean = true,
    )

    @Serializable
    data class LogsTableColumnsWidth(
        val time: Double = 150.0,
        val type: Double = 200.0,
        val status: Double = 200.0,
        val message: Double = 300.0,
    )

    @Serializable
    data class ThreadsTableColumns(
        val title: Boolean = true,
        val link: Boolean = true,
        val count: Boolean = true,
    )

    @Serializable
    data class ThreadsTableColumnsWidth(
        val title: Double = 350.0,
        val link: Double = 350.0,
        val count: Double = 100.0,
    )

    @Serializable
    data class PostsTableColumns(
        val preview: Boolean = true,
        val title: Boolean = true,
        val progress: Boolean = true,
        val status: Boolean = true,
        val path: Boolean = true,
        val total: Boolean = true,
        val hosts: Boolean = true,
        val addedOn: Boolean = true,
        val order: Boolean = true,
    )

    @Serializable
    data class PostsTableColumnsWidth(
        val preview: Double = 100.0,
        val title: Double = 250.0,
        val progress: Double = 100.0,
        val status: Double = 100.0,
        val path: Double = 250.0,
        val total: Double = 150.0,
        val hosts: Double = 100.0,
        val addedOn: Double = 125.0,
        val order: Double = 100.0,
    )

    @Serializable
    data class ImagesTableColumns(
        val preview: Boolean = true,
        val index: Boolean = true,
        val link: Boolean = true,
        val progress: Boolean = true,
        val filename: Boolean = true,
        val status: Boolean = true,
        val size: Boolean = true,
        val downloaded: Boolean = true,
    )

    @Serializable
    data class ImagesTableColumnsWidth(
        val preview: Double = 100.0,
        val index: Double = 100.0,
        val link: Double = 200.0,
        val progress: Double = 100.0,
        val filename: Double = 150.0,
        val status: Double = 100.0,
        val size: Double = 75.0,
        val downloaded: Double = 125.0,
    )

    @Serializable
    data class RemoteSession(
        val host: String = "",
        val port: Int = 30000,
    )

    @Serializable
    data class WidgetsSettings(
        val firstRun: Boolean = true,
        val localSession: Boolean = true,
        val visibleInfoPanel: Boolean = true,
        val visibleToolbarPanel: Boolean = true,
        val visibleStatusBarPanel: Boolean = true,
        val darkMode: Boolean = false,
        val infoPanelDividerPosition: Double = 0.7,
        val width: Double = 1366.0,
        val height: Double = 768.0,
        val cachePath: String = System.getProperty("java.io.tmpdir"),
        val postsTableColumns: PostsTableColumns = PostsTableColumns(),
        val imagesTableColumns: ImagesTableColumns = ImagesTableColumns(),
        val threadsTableColumns: ThreadsTableColumns = ThreadsTableColumns(),
        val logsTableColumns: LogsTableColumns = LogsTableColumns(),
        val threadSelectionColumns: ThreadSelectionTableColumns = ThreadSelectionTableColumns(),
        val remoteSession: RemoteSession = RemoteSession(),
        val postsTableColumnsWidth: PostsTableColumnsWidth = PostsTableColumnsWidth(),
        val imagesTableColumnsWidth: ImagesTableColumnsWidth = ImagesTableColumnsWidth(),
        val threadsTableColumnsWidth: ThreadsTableColumnsWidth = ThreadsTableColumnsWidth(),
        val threadSelectionColumnsWidth: ThreadSelectionTableColumnsWidth = ThreadSelectionTableColumnsWidth(),
        val logsTableColumnsWidth: LogsTableColumnsWidth = LogsTableColumnsWidth(),
    )

    var currentSettings: WidgetsViewModel = loadSettings().let {
        WidgetsViewModel(
            it.firstRun,
            it.localSession,
            it.visibleInfoPanel,
            it.visibleToolbarPanel,
            it.visibleStatusBarPanel,
            it.darkMode,
            it.infoPanelDividerPosition,
            it.width,
            it.height,
            it.cachePath,
            it.postsTableColumns,
            it.imagesTableColumns,
            it.threadsTableColumns,
            it.logsTableColumns,
            it.threadSelectionColumns,
            it.remoteSession,
            it.postsTableColumnsWidth,
            it.imagesTableColumnsWidth,
            it.threadsTableColumnsWidth,
            it.threadSelectionColumnsWidth,
            it.logsTableColumnsWidth
        )
    }

    init {
        currentSettings.firstRunProperty.onChange {
            update()
        }
        currentSettings.localSessionProperty.onChange {
            update()
        }
        currentSettings.visibleInfoPanelProperty.onChange {
            update()
        }
        currentSettings.visibleToolbarPanelProperty.onChange {
            update()
        }
        currentSettings.visibleStatusBarPanelProperty.onChange {
            update()
        }
        currentSettings.darkModeProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.previewProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.progressProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.addedOnProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.hostsProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.orderProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.pathProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.statusProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.titleProperty.onChange {
            update()
        }
        currentSettings.postsColumnsModel.totalProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.previewProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.progressProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.addedOnProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.hostsProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.orderProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.pathProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.statusProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.titleProperty.onChange {
            update()
        }
        currentSettings.postsColumnsWidthModel.totalProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.previewProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.indexProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.linkProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.progressProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.filenameProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.statusProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.sizeProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsModel.downloadedProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.previewProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.indexProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.linkProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.progressProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.filenameProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.statusProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.sizeProperty.onChange {
            update()
        }
        currentSettings.imagesColumnsWidthModel.downloadedProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsModel.titleProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsModel.linkProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsModel.countProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsWidthModel.titleProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsWidthModel.linkProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsWidthModel.countProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsModel.previewProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsModel.indexProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsModel.titleProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsModel.linkProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsModel.hostsProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsWidthModel.previewProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsWidthModel.indexProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsWidthModel.titleProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsWidthModel.linkProperty.onChange {
            update()
        }
        currentSettings.threadSelectionColumnsWidthModel.hostsProperty.onChange {
            update()
        }
        currentSettings.logsColumnsModel.timeProperty.onChange {
            update()
        }
        currentSettings.logsColumnsModel.typeProperty.onChange {
            update()
        }
        currentSettings.logsColumnsModel.statusProperty.onChange {
            update()
        }
        currentSettings.logsColumnsModel.messageProperty.onChange {
            update()
        }
        currentSettings.logsColumnsWidthModel.timeProperty.onChange {
            update()
        }
        currentSettings.logsColumnsWidthModel.typeProperty.onChange {
            update()
        }
        currentSettings.logsColumnsWidthModel.statusProperty.onChange {
            update()
        }
        currentSettings.logsColumnsWidthModel.messageProperty.onChange {
            update()
        }
        currentSettings.remoteSessionModel.hostProperty.onChange {
            update()
        }
        currentSettings.remoteSessionModel.portProperty.onChange {
            update()
        }
        currentSettings.cachePathProperty.onChange {
            update()
        }
    }

    private fun loadSettings(): WidgetsSettings {
        if (!Files.exists(WIDGETS_CONFIG_PATH)) {
            val widgetsSettings = WidgetsSettings()
            currentSettings = WidgetsViewModel(
                widgetsSettings.firstRun,
                widgetsSettings.localSession,
                widgetsSettings.visibleInfoPanel,
                widgetsSettings.visibleToolbarPanel,
                widgetsSettings.visibleStatusBarPanel,
                widgetsSettings.darkMode,
                widgetsSettings.infoPanelDividerPosition,
                widgetsSettings.width,
                widgetsSettings.height,
                widgetsSettings.cachePath,
                widgetsSettings.postsTableColumns,
                widgetsSettings.imagesTableColumns,
                widgetsSettings.threadsTableColumns,
                widgetsSettings.logsTableColumns,
                widgetsSettings.threadSelectionColumns,
                widgetsSettings.remoteSession,
                widgetsSettings.postsTableColumnsWidth,
                widgetsSettings.imagesTableColumnsWidth,
                widgetsSettings.threadsTableColumnsWidth,
                widgetsSettings.threadSelectionColumnsWidth,
                widgetsSettings.logsTableColumnsWidth
            )
            synchronized(this) {
                update()
            }
        }
        return (json.decodeFromString(Files.readString(WIDGETS_CONFIG_PATH)) as WidgetsSettings)

    }

    @Synchronized
    fun update() {
        Files.writeString(
            WIDGETS_CONFIG_PATH, json.encodeToString(
                WidgetsSettings(
                    currentSettings.firstRun,
                    currentSettings.localSession,
                    currentSettings.visibleInfoPanel,
                    currentSettings.visibleToolbarPanel,
                    currentSettings.visibleStatusBarPanel,
                    currentSettings.darkMode,
                    currentSettings.infoPanelDividerPosition,
                    currentSettings.width,
                    currentSettings.height,
                    currentSettings.cachePath,
                    PostsTableColumns(
                        currentSettings.postsColumnsModel.preview,
                        currentSettings.postsColumnsModel.title,
                        currentSettings.postsColumnsModel.progress,
                        currentSettings.postsColumnsModel.status,
                        currentSettings.postsColumnsModel.path,
                        currentSettings.postsColumnsModel.total,
                        currentSettings.postsColumnsModel.hosts,
                        currentSettings.postsColumnsModel.addedOn,
                        currentSettings.postsColumnsModel.order,
                    ),
                    ImagesTableColumns(
                        currentSettings.imagesColumnsModel.preview,
                        currentSettings.imagesColumnsModel.index,
                        currentSettings.imagesColumnsModel.link,
                        currentSettings.imagesColumnsModel.progress,
                        currentSettings.imagesColumnsModel.filename,
                        currentSettings.imagesColumnsModel.status,
                        currentSettings.imagesColumnsModel.size,
                        currentSettings.imagesColumnsModel.downloaded,
                    ),
                    ThreadsTableColumns(
                        currentSettings.threadsColumnsModel.title,
                        currentSettings.threadsColumnsModel.link,
                        currentSettings.threadsColumnsModel.count,
                    ),
                    LogsTableColumns(
                        currentSettings.logsColumnsModel.time,
                        currentSettings.logsColumnsModel.type,
                        currentSettings.logsColumnsModel.status,
                        currentSettings.logsColumnsModel.message,
                    ),
                    ThreadSelectionTableColumns(
                        currentSettings.threadSelectionColumnsModel.preview,
                        currentSettings.threadSelectionColumnsModel.index,
                        currentSettings.threadSelectionColumnsModel.title,
                        currentSettings.threadSelectionColumnsModel.link,
                        currentSettings.threadSelectionColumnsModel.hosts,
                    ),
                    RemoteSession(
                        currentSettings.remoteSessionModel.host,
                        currentSettings.remoteSessionModel.port
                    ),
                    PostsTableColumnsWidth(
                        currentSettings.postsColumnsWidthModel.preview,
                        currentSettings.postsColumnsWidthModel.title,
                        currentSettings.postsColumnsWidthModel.progress,
                        currentSettings.postsColumnsWidthModel.status,
                        currentSettings.postsColumnsWidthModel.path,
                        currentSettings.postsColumnsWidthModel.total,
                        currentSettings.postsColumnsWidthModel.hosts,
                        currentSettings.postsColumnsWidthModel.addedOn,
                        currentSettings.postsColumnsWidthModel.order,
                    ),
                    ImagesTableColumnsWidth(
                        currentSettings.imagesColumnsWidthModel.preview,
                        currentSettings.imagesColumnsWidthModel.index,
                        currentSettings.imagesColumnsWidthModel.link,
                        currentSettings.imagesColumnsWidthModel.progress,
                        currentSettings.imagesColumnsWidthModel.filename,
                        currentSettings.imagesColumnsWidthModel.status,
                        currentSettings.imagesColumnsWidthModel.size,
                        currentSettings.imagesColumnsWidthModel.downloaded,
                    ),
                    ThreadsTableColumnsWidth(
                        currentSettings.threadsColumnsWidthModel.title,
                        currentSettings.threadsColumnsWidthModel.link,
                        currentSettings.threadsColumnsWidthModel.count,
                    ),
                    ThreadSelectionTableColumnsWidth(
                        currentSettings.threadSelectionColumnsWidthModel.preview,
                        currentSettings.threadSelectionColumnsWidthModel.index,
                        currentSettings.threadSelectionColumnsWidthModel.title,
                        currentSettings.threadSelectionColumnsWidthModel.link,
                        currentSettings.threadSelectionColumnsWidthModel.hosts,
                    ),
                    LogsTableColumnsWidth(
                        currentSettings.logsColumnsWidthModel.time,
                        currentSettings.logsColumnsWidthModel.type,
                        currentSettings.logsColumnsWidthModel.status,
                        currentSettings.logsColumnsWidthModel.message,
                    ),
                )
            )
        )
    }
}