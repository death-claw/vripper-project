package me.mnlr.vripper.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import me.mnlr.vripper.host.Host
import tornadofx.*

class ThreadSelectionModel(
    index: Int,
    title: String,
    url: String,
    hosts: Map<Host, Int>,
    val postId: String,
    val threadId: String
) {
    val indexProperty = SimpleIntegerProperty(index)
    var index: Int by indexProperty

    val titleProperty = SimpleStringProperty(title)
    var title: String by titleProperty

    val urlProperty = SimpleStringProperty(url)
    var url: String by urlProperty

    val hostsProperty = SimpleStringProperty(hosts.keys.map { it.host }.joinToString(", "))
    var hosts: String by hostsProperty
}
