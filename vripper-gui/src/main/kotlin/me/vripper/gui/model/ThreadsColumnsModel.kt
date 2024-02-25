package me.vripper.gui.model

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.getValue
import tornadofx.setValue

class ThreadsColumnsModel(
    title: Boolean,
    link: Boolean,
    count: Boolean,
) {
    val titleProperty = SimpleBooleanProperty(title)
    var title: Boolean by titleProperty

    val linkProperty = SimpleBooleanProperty(link)
    var link: Boolean by linkProperty

    val countProperty = SimpleBooleanProperty(count)
    var count: Boolean by countProperty
}