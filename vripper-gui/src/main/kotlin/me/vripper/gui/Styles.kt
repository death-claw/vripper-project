package me.vripper.gui

import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.Stylesheet
import tornadofx.box
import tornadofx.cssclass
import tornadofx.px

class Styles : Stylesheet() {
    companion object {
        val heading by cssclass()
        val actionBarButton by cssclass()
    }

    init {
        label and heading {
            padding = box(20.px)
            fontSize = 20.px
            fontWeight = FontWeight.BOLD
        }

        actionBarButton {
            prefWidth = 32.px
            prefHeight = 32.px
            padding = box(0.px)
            backgroundColor += Color.TRANSPARENT
            and(hover) {
                backgroundColor += Color.LIGHTGREY
            }
        }
    }
}