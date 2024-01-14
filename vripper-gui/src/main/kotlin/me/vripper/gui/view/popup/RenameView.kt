package me.vripper.gui.view.popup

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import me.vripper.gui.controller.PostController
import tornadofx.*

class RenameView : Fragment("Rename download post") {

    val postId: Long by param()
    val name: String by param()
    private val textAreaProperty = SimpleStringProperty()
    private val postController: PostController by inject()
    lateinit var input: TextField

    override fun onDock() {
        textAreaProperty.value = name
    }

    override val root = vbox(alignment = Pos.CENTER_RIGHT) {
        padding = insets(all = 5)
        spacing = 5.0
        input = textfield {
            VBox.setVgrow(this, Priority.ALWAYS)
            bind(textAreaProperty)
        }
        button("Ok") {
            imageview("search.png") {
                fitWidth = 18.0
                fitHeight = 18.0
            }
            disableWhen(textAreaProperty.isEmpty)
            action {
                postController.rename(postId, textAreaProperty.value)
                close()
            }
        }
    }
}