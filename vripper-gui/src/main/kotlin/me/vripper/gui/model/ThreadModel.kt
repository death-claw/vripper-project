package me.vripper.gui.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.getValue
import tornadofx.setValue

class ThreadModel(
    title: String,
    link: String,
    total: Int,
    val threadId: Long
) {
    val titleProperty = SimpleStringProperty(title)
    var title: String by titleProperty

    val linkProperty = SimpleStringProperty(link)
    var link: String by linkProperty

    val totalProperty = SimpleIntegerProperty(total)
    var total: Int by totalProperty
}
