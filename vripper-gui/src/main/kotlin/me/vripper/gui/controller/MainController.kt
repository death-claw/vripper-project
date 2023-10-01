package me.vripper.gui.controller

import me.vripper.utilities.ApplicationProperties
import tornadofx.Controller

class MainController : Controller() {
    val version = ApplicationProperties.VERSION
}