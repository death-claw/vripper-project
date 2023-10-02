package me.mnlr.vripper.gui.model

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class PostModel(
    postId: String,
    title: String,
    progress: Double,
    status: String,
    url: String,
    done: Int,
    total: Int,
    hosts: String,
    addedOn: String,
    order: Int,
    path: String,
    progressCount: String
) {
    val postIdProperty = SimpleStringProperty(postId)
    var postId: String by postIdProperty

    val titleProperty = SimpleStringProperty(title)
    var title: String by titleProperty

    val progressProperty = SimpleDoubleProperty(progress)
    var progress: Double by progressProperty

    val statusProperty = SimpleStringProperty(status)
    var status: String by statusProperty

    val urlProperty = SimpleStringProperty(url)
    var url: String by urlProperty

    val doneProperty = SimpleIntegerProperty(done)
    var done: Int by doneProperty

    val totalProperty = SimpleIntegerProperty(total)
    var total: Int by totalProperty

    val hostsProperty = SimpleStringProperty(hosts)
    var hosts: String by hostsProperty

    val addedOnProperty = SimpleStringProperty(addedOn)
    var addedOn: String by addedOnProperty

    val orderProperty = SimpleIntegerProperty(order)
    var order: Int by orderProperty

    val pathProperty = SimpleStringProperty(path)
    var path: String by pathProperty

    val progressCountProperty = SimpleStringProperty(progressCount)
    var progressCount: String by progressCountProperty
}
