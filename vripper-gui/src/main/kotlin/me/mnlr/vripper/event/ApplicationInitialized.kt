package me.mnlr.vripper.event

import tornadofx.*
import tornadofx.EventBus

object ApplicationInitialized : FXEvent(EventBus.RunOn.BackgroundThread)