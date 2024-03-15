package me.vripper.gui.components.views

import io.grpc.ConnectivityState
import javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS
import javafx.scene.effect.DropShadow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterIsInstance
import me.vripper.gui.VripperGuiApplication
import me.vripper.gui.components.fragments.SessionFragment
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.services.DatabaseManager
import me.vripper.gui.services.GrpcEndpointService
import tornadofx.*

class LoadingView : View("VRipper") {

    private val widgetsController: WidgetsController by inject()
    private val grpcEndpointService: GrpcEndpointService by di("remoteAppEndpointService")
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob())

    override val root = borderpane {}

    init {
        coroutineScope.launch {
            GuiEventBus.events.filterIsInstance(GuiEventBus.ApplicationInitialized::class).collect {
                runLater {
                    if (widgetsController.currentSettings.firstRun) {
                        widgetsController.currentSettings.firstRun = false
                        val sessionView = find<SessionFragment>(mapOf(SessionFragment::component to this@LoadingView))
                        sessionView.openModal().also {
                            it?.setOnCloseRequest {
                                VripperGuiApplication.APP_INSTANCE.stop()
                            }
                        }
                    } else {
                        if (widgetsController.currentSettings.localSession) {
                            DatabaseManager.connect()
                            replaceWith(find<AppView>())
                        } else {
                            grpcEndpointService.connect(
                                widgetsController.currentSettings.remoteSessionModel.host,
                                widgetsController.currentSettings.remoteSessionModel.port
                            )

                            coroutineScope.launch {
                                repeat(10) {
                                    if (grpcEndpointService.connectionState() == ConnectivityState.READY) {
                                        runLater {
                                            replaceWith(find<AppView>())
                                        }
                                        cancel()
                                    }
                                    delay(333)
                                }
                                runLater {
                                    val sessionView = find<SessionFragment>(
                                        mapOf(SessionFragment::component to this@LoadingView)
                                    )
                                    sessionView.openModal().also {
                                        it?.setOnCloseRequest {
                                            VripperGuiApplication.APP_INSTANCE.stop()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        with(root) {
            padding = insets(all = 5)
            center {
                progressindicator {
                    progress = INDETERMINATE_PROGRESS
                }
            }
            effect = DropShadow()
        }
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}