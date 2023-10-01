package me.vripper.gui.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class ThreadSelectionModel(
    index: Int,
    title: String,
    url: String,
    hosts: List<Pair<String, Int>>,
    val postId: Long,
    val threadId: Long,
    previewList: List<String>
) {
    val indexProperty = SimpleIntegerProperty(index)
    var index: Int by indexProperty

    val titleProperty = SimpleStringProperty(title)
    var title: String by titleProperty

    val urlProperty = SimpleStringProperty(url)
    var url: String by urlProperty

    val hostsProperty = SimpleStringProperty(hosts.joinToString(", ") { "${it.first} (${it.second})" })
    var hosts: String by hostsProperty

    val previewListProperty = SimpleStringProperty(previewList.joinToString("|"))
    var previewList: List<String> = previewListProperty.value.split("|")
}
