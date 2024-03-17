package me.vripper.gui.components.views

import io.grpc.ConnectivityState
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Orientation
import javafx.geometry.Pos
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import me.vripper.delegate.LoggerDelegate
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.services.GrpcEndpointService
import me.vripper.services.AppEndpointService
import me.vripper.services.IAppEndpointService
import me.vripper.utilities.formatSI
import tornadofx.*

class StatusBarView : View("Status bar") {
    private val logger by LoggerDelegate()
    private val widgetsController: WidgetsController by inject()
    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val grpcEndpointService: GrpcEndpointService by di("remoteAppEndpointService")
    private val localEndpointService: AppEndpointService by di("localAppEndpointService")
    private val remoteText = SimpleStringProperty()
    private val loggedUser = SimpleStringProperty()
    private val tasksRunning = SimpleBooleanProperty(false)
    private val downloadSpeed = SimpleStringProperty(0L.formatSI())
    private val running = SimpleIntegerProperty(0)
    private val pending = SimpleIntegerProperty(0)
    private val error = SimpleIntegerProperty(0)
    private val jobs = mutableListOf<Job>()

    init {
        coroutineScope.launch {
            GuiEventBus.events.collect { event ->
                when (event) {
                    is GuiEventBus.LocalSession -> {
                        connect(localEndpointService)
                    }

                    is GuiEventBus.RemoteSession -> {
                        connect(grpcEndpointService)
                    }

                    is GuiEventBus.ChangingSession -> {
                        jobs.forEach { it.cancel() }
                        jobs.clear()
                    }
                }
            }
        }
    }

    private fun connect(endpointService: IAppEndpointService) {
        coroutineScope.launch {
            val user = async { endpointService.loggedInUser() }.await()
            runLater {
                loggedUser.set(user)
            }
        }

        coroutineScope.launch {
            endpointService.onVGUserUpdate().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.collect {
                runLater {
                    loggedUser.set(it)
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            endpointService.onTasksRunning().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.collect {
                runLater {
                    tasksRunning.set(it)
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            endpointService.onDownloadSpeed().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.collect {
                runLater {
                    downloadSpeed.set(it.formatSI())
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            endpointService.onQueueStateUpdate().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.collect {
                runLater {
                    running.set(it.running)
                    pending.set(it.remaining)
                }
            }
        }.also { jobs.add(it) }

        coroutineScope.launch {
            endpointService.onErrorCountUpdate().catch {
                logger.error("gRPC error", it)
                currentCoroutineContext().cancel(null)
            }.collect {
                runLater {
                    error.set(it)
                }
            }
        }.also { jobs.add(it) }
        if (!widgetsController.currentSettings.localSession) {
            coroutineScope.launch {
                while (isActive) {
                    if (grpcEndpointService.connectionState() != ConnectivityState.READY) {
                        runLater {
                            remoteText.set("Unable to connect to ${widgetsController.currentSettings.remoteSessionModel.host}:${widgetsController.currentSettings.remoteSessionModel.port}")
                        }
                    } else {
                        val version = grpcEndpointService.getVersion()
                        runLater {
                            remoteText.set("Connected to ${widgetsController.currentSettings.remoteSessionModel.host}:${widgetsController.currentSettings.remoteSessionModel.port} v$version")
                        }
                    }
                    delay(5000)
                }
            }.also { jobs.add(it) }
        }
    }

    override val root = borderpane {
        id = "statusbar"
        left {
            hbox {
                label(remoteText) {
                    visibleWhen { widgetsController.currentSettings.localSessionProperty.not() }
                }
                separator(Orientation.VERTICAL) {
                    visibleWhen {
                        widgetsController.currentSettings.localSessionProperty.not().and(loggedUser.isNotBlank())
                    }
                }
                label(loggedUser.map { "Logged in as: $it" }) {
                    visibleWhen(loggedUser.isNotBlank())
                }
            }
        }
        right {
            padding = insets(right = 5, left = 5, top = 3, bottom = 3)
            hbox {
                spacing = 3.0
                progressbar {
                    visibleWhen(tasksRunning)
                }
                separator(Orientation.VERTICAL) {
                    visibleWhen(tasksRunning)
                }
                label(downloadSpeed.map { "$it/s" })
                separator(Orientation.VERTICAL)
                label("Downloading")
                label(running.asString())
                separator(Orientation.VERTICAL)
                label("Pending")
                label(pending.asString())
                separator(Orientation.VERTICAL)
                label("Error")
                label(error.asString())
                alignment = Pos.CENTER_RIGHT
            }
        }
    }
}
