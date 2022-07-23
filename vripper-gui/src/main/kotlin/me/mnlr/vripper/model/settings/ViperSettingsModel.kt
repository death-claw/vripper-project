package me.mnlr.vripper.model.settings

import javafx.beans.property.*
import tornadofx.*

class ViperSettingsModel {
    val loginProperty = SimpleBooleanProperty()
    var login: Boolean by loginProperty

    val usernameProperty = SimpleStringProperty()
    var username: String by usernameProperty

    val passwordProperty = SimpleStringProperty()
    var password: String by passwordProperty

    val thanksProperty = SimpleBooleanProperty()
    var thanks: Boolean by thanksProperty

    val hostProperty = SimpleStringProperty()
    var host: String by hostProperty
}
