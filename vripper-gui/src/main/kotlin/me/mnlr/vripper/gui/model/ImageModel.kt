package me.mnlr.vripper.gui.model

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class ImageModel(
    id: Long,
    index: Int,
    url: String,
    progress: Double,
    status: String
) {
    val idProperty = SimpleLongProperty(id)
    var id: Long by idProperty

    val indexProperty = SimpleIntegerProperty(index)
    var index: Int by indexProperty

    val urlProperty = SimpleStringProperty(url)
    var url: String by urlProperty

    val progressProperty = SimpleDoubleProperty(progress)
    var progress: Double by progressProperty

    val statusProperty = SimpleStringProperty(status)
    var status: String by statusProperty
}
