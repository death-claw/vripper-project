package me.mnlr.vripper.controller

import me.mnlr.vripper.services.AppVersion
import tornadofx.*

class MainController : Controller() {
    val version = AppVersion.VERSION
}