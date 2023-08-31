package me.mnlr.vripper.model.settings

import javafx.beans.property.*
import tornadofx.*

class DownloadSettingsModel {
    val downloadPathProperty = SimpleStringProperty()
    var downloadPath: String by downloadPathProperty

    val tempPathProperty = SimpleStringProperty()
    var tempPath: String by tempPathProperty

    val autoStartProperty = SimpleBooleanProperty()
    var autoStart: Boolean by autoStartProperty

    val autoQueueThresholdProperty = SimpleIntegerProperty()
    var autoQueueThreshold: Int by autoQueueThresholdProperty

    val forceOrderProperty = SimpleBooleanProperty()
    var forceOrder: Boolean by forceOrderProperty

    val forumSubfolderProperty = SimpleBooleanProperty()
    var forumSubfolder: Boolean by forumSubfolderProperty

    val threadSubLocationProperty = SimpleBooleanProperty()
    var threadSubLocation: Boolean by threadSubLocationProperty

    val clearCompletedProperty = SimpleBooleanProperty()
    var clearCompleted: Boolean by clearCompletedProperty

    val appendPostIdProperty = SimpleBooleanProperty()
    var appendPostId: Boolean by appendPostIdProperty
}
