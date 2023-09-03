package me.mnlr.vripper.controller

import me.mnlr.vripper.services.AppVersion
import tornadofx.*

class MainController : Controller() {
    private val appVersion: AppVersion by di()

    val version = appVersion.version
    val timestamp = appVersion.timestamp
}