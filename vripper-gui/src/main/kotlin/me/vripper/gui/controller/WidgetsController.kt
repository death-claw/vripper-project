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
    data class LogsTableColumns(
        val time: Boolean = true,
        val type: Boolean = true,
        val status: Boolean = true,
        val message: Boolean = true,
    )

    @Serializable
    data class ThreadsTableColumns(
        val title: Boolean = true,
        val link: Boolean = true,
        val count: Boolean = true,
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
    data class WidgetsSettings(
        val visibleInfoPanel: Boolean = true,
        val visibleToolbarPanel: Boolean = true,
        val visibleStatusBarPanel: Boolean = true,
        val infoPanelDividerPosition: Double = 0.7,
        val width: Double = 1366.0,
        val height: Double = 768.0,
        val postsTableColumns: PostsTableColumns = PostsTableColumns(),
        val imagesTableColumns: ImagesTableColumns = ImagesTableColumns(),
        val threadsTableColumns: ThreadsTableColumns = ThreadsTableColumns(),
        val logsTableColumns: LogsTableColumns = LogsTableColumns(),
        val threadSelectionColumns: ThreadSelectionTableColumns = ThreadSelectionTableColumns(),
    )

    var currentSettings: WidgetsViewModel = loadSettings().let {
        WidgetsViewModel(
            it.visibleInfoPanel,
            it.visibleToolbarPanel,
            it.visibleStatusBarPanel,
            it.infoPanelDividerPosition,
            it.width,
            it.height,
            it.postsTableColumns,
            it.imagesTableColumns,
            it.threadsTableColumns,
            it.logsTableColumns,
            it.threadSelectionColumns
        )
    }

    init {
        currentSettings.visibleInfoPanelProperty.onChange {
            update()
        }
        currentSettings.visibleToolbarPanelProperty.onChange {
            update()
        }
        currentSettings.visibleStatusBarPanelProperty.onChange {
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
        currentSettings.threadsColumnsModel.titleProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsModel.linkProperty.onChange {
            update()
        }
        currentSettings.threadsColumnsModel.countProperty.onChange {
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
    }

    private fun loadSettings(): WidgetsSettings {
        if (!Files.exists(WIDGETS_CONFIG_PATH)) {
            val widgetsSettings = WidgetsSettings()
            currentSettings = WidgetsViewModel(
                widgetsSettings.visibleInfoPanel,
                widgetsSettings.visibleToolbarPanel,
                widgetsSettings.visibleStatusBarPanel,
                widgetsSettings.infoPanelDividerPosition,
                widgetsSettings.width,
                widgetsSettings.height,
                widgetsSettings.postsTableColumns,
                widgetsSettings.imagesTableColumns,
                widgetsSettings.threadsTableColumns,
                widgetsSettings.logsTableColumns,
                widgetsSettings.threadSelectionColumns
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
                    currentSettings.visibleInfoPanel,
                    currentSettings.visibleToolbarPanel,
                    currentSettings.visibleStatusBarPanel,
                    currentSettings.infoPanelDividerPosition,
                    currentSettings.width,
                    currentSettings.height,
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
                    )
                )
            )
        )
    }
}