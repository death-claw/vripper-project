package me.vripper.gui.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import me.vripper.gui.controller.WidgetsController
import tornadofx.getValue
import tornadofx.setValue

class WidgetsViewModel(
    firstRun: Boolean,
    localSession: Boolean,
    visibleInfoPanel: Boolean,
    visibleToolbarPanel: Boolean,
    visibleStatusBarPanel: Boolean,
    darkMode: Boolean,
    infoPanelDividerPosition: Double,
    width: Double,
    height: Double,
    cachePath: String,
    postsTableColumns: WidgetsController.PostsTableColumns,
    imagesTableColumns: WidgetsController.ImagesTableColumns,
    threadsTableColumns: WidgetsController.ThreadsTableColumns,
    logsTableColumns: WidgetsController.LogsTableColumns,
    threadSelectionColumns: WidgetsController.ThreadSelectionTableColumns,
    remoteSession: WidgetsController.RemoteSession,
    postsTableColumnsWidth: WidgetsController.PostsTableColumnsWidth,
    imagesTableColumnsWidth: WidgetsController.ImagesTableColumnsWidth,
    threadsTableColumnsWidth: WidgetsController.ThreadsTableColumnsWidth,
    threadSelectionColumnsWidth: WidgetsController.ThreadSelectionTableColumnsWidth,
    logsTableColumnsWidth: WidgetsController.LogsTableColumnsWidth,
) {
    val firstRunProperty = SimpleBooleanProperty(firstRun)
    var firstRun: Boolean by firstRunProperty

    val localSessionProperty = SimpleBooleanProperty(localSession)
    var localSession: Boolean by localSessionProperty

    val visibleInfoPanelProperty = SimpleBooleanProperty(visibleInfoPanel)
    var visibleInfoPanel: Boolean by visibleInfoPanelProperty

    val visibleToolbarPanelProperty = SimpleBooleanProperty(visibleToolbarPanel)
    var visibleToolbarPanel: Boolean by visibleToolbarPanelProperty

    val visibleStatusBarPanelProperty = SimpleBooleanProperty(visibleStatusBarPanel)
    var visibleStatusBarPanel: Boolean by visibleStatusBarPanelProperty

    val infoPanelDividerPositionProperty = SimpleDoubleProperty(infoPanelDividerPosition)
    var infoPanelDividerPosition: Double by infoPanelDividerPositionProperty

    val darkModeProperty = SimpleBooleanProperty(darkMode)
    var darkMode: Boolean by darkModeProperty

    val widthProperty = SimpleDoubleProperty(width)
    var width: Double by widthProperty

    val heightProperty = SimpleDoubleProperty(height)
    var height: Double by heightProperty

    val cachePathProperty = SimpleStringProperty(cachePath)
    var cachePath: String by cachePathProperty

    val postsColumnsModel = PostsColumnsModel(
        postsTableColumns.preview,
        postsTableColumns.title,
        postsTableColumns.progress,
        postsTableColumns.status,
        postsTableColumns.path,
        postsTableColumns.total,
        postsTableColumns.hosts,
        postsTableColumns.addedOn,
        postsTableColumns.order,
    )

    val postsColumnsWidthModel = PostsColumnsWidthModel(
        postsTableColumnsWidth.preview,
        postsTableColumnsWidth.title,
        postsTableColumnsWidth.progress,
        postsTableColumnsWidth.status,
        postsTableColumnsWidth.path,
        postsTableColumnsWidth.total,
        postsTableColumnsWidth.hosts,
        postsTableColumnsWidth.addedOn,
        postsTableColumnsWidth.order,
    )

    val imagesColumnsModel = ImagesColumnsModel(
        imagesTableColumns.preview,
        imagesTableColumns.index,
        imagesTableColumns.link,
        imagesTableColumns.progress,
        imagesTableColumns.filename,
        imagesTableColumns.status,
        imagesTableColumns.size,
        imagesTableColumns.downloaded,
    )

    val imagesColumnsWidthModel = ImagesColumnsWidthModel(
        imagesTableColumnsWidth.preview,
        imagesTableColumnsWidth.index,
        imagesTableColumnsWidth.link,
        imagesTableColumnsWidth.progress,
        imagesTableColumnsWidth.filename,
        imagesTableColumnsWidth.status,
        imagesTableColumnsWidth.size,
        imagesTableColumnsWidth.downloaded,
    )

    val threadsColumnsModel = ThreadsColumnsModel(
        threadsTableColumns.title,
        threadsTableColumns.link,
        threadsTableColumns.count,
    )

    val threadsColumnsWidthModel = ThreadsColumnsWidthModel(
        threadsTableColumnsWidth.title,
        threadsTableColumnsWidth.link,
        threadsTableColumnsWidth.count,
    )

    val logsColumnsModel = LogsColumnsModel(
        logsTableColumns.time,
        logsTableColumns.type,
        logsTableColumns.status,
        logsTableColumns.message,
    )

    val logsColumnsWidthModel = LogsColumnsWidthModel(
        logsTableColumnsWidth.time,
        logsTableColumnsWidth.type,
        logsTableColumnsWidth.status,
        logsTableColumnsWidth.message,
    )

    val threadSelectionColumnsModel = ThreadSelectionColumnsModel(
        threadSelectionColumns.preview,
        threadSelectionColumns.index,
        threadSelectionColumns.title,
        threadSelectionColumns.link,
        threadSelectionColumns.hosts,
    )

    val threadSelectionColumnsWidthModel = ThreadSelectionColumnsWidthModel(
        threadSelectionColumnsWidth.preview,
        threadSelectionColumnsWidth.index,
        threadSelectionColumnsWidth.title,
        threadSelectionColumnsWidth.link,
        threadSelectionColumnsWidth.hosts,
    )

    val remoteSessionModel = RemoteSessionModel(
        remoteSession.host,
        remoteSession.port
    )
}