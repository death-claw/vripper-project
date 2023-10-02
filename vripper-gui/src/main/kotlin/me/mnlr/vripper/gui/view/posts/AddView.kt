package me.mnlr.vripper.gui.view.posts

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import me.mnlr.vripper.gui.controller.PostController
import tornadofx.*

class AddView : Fragment("Add thread links") {

    private val textAreaProperty = SimpleStringProperty()
    private val postController: PostController by inject()
    lateinit var input: TextArea

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        padding = insets(all = 5)
        spacing = 5.0
        input = textarea {
            VBox.setVgrow(this, Priority.ALWAYS)
            bind(textAreaProperty)
        }
        button("Scan") {
            imageview("search.png") {
                fitWidth = 18.0
                fitHeight = 18.0
            }
            disableWhen(textAreaProperty.isEmpty)
            action {
                postController.scan(textAreaProperty.value)
                close()
            }
        }
    }
}