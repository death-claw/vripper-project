package me.vripper.gui.model

import javafx.beans.property.SimpleDoubleProperty
import tornadofx.getValue
import tornadofx.setValue

class ThreadsColumnsWidthModel(
    title: Double,
    link: Double,
    count: Double,
) {
    val titleProperty = SimpleDoubleProperty(title)
    var title: Double by titleProperty

    val linkProperty = SimpleDoubleProperty(link)
    var link: Double by linkProperty

    val countProperty = SimpleDoubleProperty(count)
    var count: Double by countProperty
}