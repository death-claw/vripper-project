package me.vripper.gui.components.views

import atlantafx.base.theme.CupertinoDark
import atlantafx.base.theme.CupertinoLight
import io.grpc.ConnectivityState
import javafx.application.Application
import kotlinx.coroutines.*
import me.vripper.gui.components.fragments.SessionFragment
import me.vripper.gui.controller.MainController
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.listener.GuiStartupLister
import me.vripper.gui.services.GrpcEndpointService
import tornadofx.View
import tornadofx.onChange
import tornadofx.runLater
import tornadofx.vbox

class AppView : View() {

    override val root = vbox { }
    private val mainController: MainController by inject()
    private val menuBarView: MenuBarView by inject()
    private val mainView: MainView by inject()
    private val statusBarView: StatusBarView by inject()
    private val actionBarView: ActionBarView by inject()
    private val widgetsController: WidgetsController by inject()
    private val grpcEndpointService: GrpcEndpointService by di("remoteAppEndpointService")
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob())
    private var job: Job? = null
    init {
        title = "VRipper ${mainController.version}"
        with(root) {
            add(menuBarView)
            if (widgetsController.currentSettings.visibleToolbarPanel) {
                add(actionBarView)
            }
            add(mainView)
            if (widgetsController.currentSettings.visibleStatusBarPanel) {
                add(statusBarView)
            }
            prefWidth = widgetsController.currentSettings.width
            prefHeight = widgetsController.currentSettings.height
        }

        widgetsController.currentSettings.visibleToolbarPanelProperty.onChange { it ->
            if (it) {
                root.children.add(1, actionBarView.root)
            } else {
                root.children.removeIf { it.id == "action_toolbar" }
            }
        }

        widgetsController.currentSettings.visibleStatusBarPanelProperty.onChange { it ->
            if (it) {
                root.children.add(statusBarView.root)
            } else {
                root.children.removeIf { it.id == "statusbar" }
            }
        }

        widgetsController.currentSettings.darkModeProperty.onChange {
            if (it) {
                Application.setUserAgentStylesheet(CupertinoDark().userAgentStylesheet)
            } else {
                Application.setUserAgentStylesheet(CupertinoLight().userAgentStylesheet)
            }
        }

        coroutineScope.launch {
            GuiEventBus.events.collect {
                when (it) {
                    is GuiEventBus.LocalSession -> {
                        job?.cancel()
                        GuiStartupLister().run()
                    }

                    is GuiEventBus.RemoteSession -> {
                        job?.cancel()
                        job = launch {
                            while (isActive) {
                                if (grpcEndpointService.connectionState() != ConnectivityState.READY) {
                                    val loadingView = find<LoadingView>()
                                    runLater {
                                        replaceWith(loadingView)
                                        find<SessionFragment>(mapOf(SessionFragment::component to loadingView)).openModal()
                                    }
                                    cancel()
                                }
                                delay(1000)
                            }
                        }
                    }
                }
            }
        }

        runLater {
            if (widgetsController.currentSettings.localSession) {
                runBlocking {
                    GuiEventBus.publishEvent(GuiEventBus.LocalSession)
                }
            } else {
                runBlocking {
                    GuiEventBus.publishEvent(GuiEventBus.RemoteSession)
                }
            }
        }
    }
}