package me.vripper.gui.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import me.vripper.gui.controller.WidgetsController
import tornadofx.getValue
import tornadofx.setValue

class WidgetsViewModel(
    visibleInfoPanel: Boolean,
    visibleToolbarPanel: Boolean,
    visibleStatusBarPanel: Boolean,
    infoPanelDividerPosition: Double,
    width: Double,
    height: Double,
    postsTableColumns: WidgetsController.PostsTableColumns,
    imagesTableColumns: WidgetsController.ImagesTableColumns,
    threadsTableColumns: WidgetsController.ThreadsTableColumns,
    logsTableColumns: WidgetsController.LogsTableColumns,
    threadSelectionColumns: WidgetsController.ThreadSelectionTableColumns,
) {
    val visibleInfoPanelProperty = SimpleBooleanProperty(visibleInfoPanel)
    var visibleInfoPanel: Boolean by visibleInfoPanelProperty

    val visibleToolbarPanelProperty = SimpleBooleanProperty(visibleToolbarPanel)
    var visibleToolbarPanel: Boolean by visibleToolbarPanelProperty

    val visibleStatusBarPanelProperty = SimpleBooleanProperty(visibleStatusBarPanel)
    var visibleStatusBarPanel: Boolean by visibleStatusBarPanelProperty

    val infoPanelDividerPositionProperty = SimpleDoubleProperty(infoPanelDividerPosition)
    var infoPanelDividerPosition: Double by infoPanelDividerPositionProperty

    val widthProperty = SimpleDoubleProperty(width)
    var width: Double by widthProperty

    val heightProperty = SimpleDoubleProperty(height)
    var height: Double by heightProperty

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

    val threadsColumnsModel = ThreadsColumnsModel(
        threadsTableColumns.title,
        threadsTableColumns.link,
        threadsTableColumns.count,
    )

    val logsColumnsModel = LogsColumnsModel(
        logsTableColumns.time,
        logsTableColumns.type,
        logsTableColumns.status,
        logsTableColumns.message,
    )

    val threadSelectionColumnsModel = ThreadSelectionColumnsModel(
        threadSelectionColumns.preview,
        threadSelectionColumns.index,
        threadSelectionColumns.title,
        threadSelectionColumns.link,
        threadSelectionColumns.hosts,
    )
}