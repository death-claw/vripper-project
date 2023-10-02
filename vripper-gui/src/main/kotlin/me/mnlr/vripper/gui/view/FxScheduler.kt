package me.mnlr.vripper.gui.view

import javafx.application.Platform
import reactor.core.scheduler.Schedulers

val FxScheduler = Schedulers.fromExecutor(Platform::runLater)