package me.vripper.gui.components.fragments

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import me.vripper.gui.model.LogModel
import tornadofx.*

class LogMessageFragment : Fragment("Log message") {

    val logModel: LogModel by param()
    private val textAreaProperty = SimpleStringProperty(logModel.message)

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        padding = insets(all = 5)
        spacing = 5.0
        textarea {
            isEditable = false
            VBox.setVgrow(this, Priority.ALWAYS)
            bind(textAreaProperty)
        }
    }
}