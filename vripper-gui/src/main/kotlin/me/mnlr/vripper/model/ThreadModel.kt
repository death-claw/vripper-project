package me.mnlr.vripper.model

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class ThreadModel(
    title: String,
    link: String,
    total: Int,
    val threadId: String
) {
    val titleProperty = SimpleStringProperty(title)
    var title: String by titleProperty

    val linkProperty = SimpleStringProperty(link)
    var link: String by linkProperty

    val totalProperty = SimpleIntegerProperty(total)
    var total: Int by totalProperty
}
