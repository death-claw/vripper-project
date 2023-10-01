package me.vripper.gui.event

import tornadofx.EventBus
import tornadofx.FXEvent

object ApplicationInitialized : FXEvent(EventBus.RunOn.BackgroundThread)