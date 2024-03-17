package me.vripper.gui.components.fragments

import atlantafx.base.theme.Styles
import atlantafx.base.util.IntegerStringConverter
import io.grpc.ConnectivityState
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.RadioButton
import javafx.scene.control.Spinner
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import kotlinx.coroutines.*
import me.vripper.gui.VripperGuiApplication
import me.vripper.gui.components.views.AppView
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.GuiEventBus
import me.vripper.gui.services.DatabaseManager
import me.vripper.gui.services.GrpcEndpointService
import tornadofx.*

class SessionFragment : Fragment("Change Session") {

    val component: UIComponent by param()
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val widgetsController: WidgetsController by inject()
    private val grpcEndpointService: GrpcEndpointService by di("remoteAppEndpointService")
    private val toggleGroup = ToggleGroup()
    private val disableConfirm = SimpleBooleanProperty(false)
    private val message = SimpleStringProperty()
    override val root = VBox().apply {
        alignment = Pos.CENTER
        padding = insets(all = 5)
        spacing = 5.0
    }

    init {
        with(root) {
            form {
                fieldset("Source") {
                    radiobutton(text = "Start Local Session", group = toggleGroup) {
                        id = "localSession"
                        isSelected = widgetsController.currentSettings.localSession
                    }
                    val remoteRadio = radiobutton(text = "Connect to Remote Session", group = toggleGroup) {
                        id = "remoteSession"
                        isSelected = !widgetsController.currentSettings.localSession
                    }
                    field("Host") {
                        enableWhen { remoteRadio.selectedProperty() }
                        textfield(widgetsController.currentSettings.remoteSessionModel.hostProperty) {}
                    }
                    field("Port") {
                        enableWhen { remoteRadio.selectedProperty() }
                        add(Spinner<Int>(0, 65535, widgetsController.currentSettings.remoteSessionModel.port).apply {
                            widgetsController.currentSettings.remoteSessionModel.portProperty.bind(valueProperty())
                            isEditable = true
                            IntegerStringConverter.createFor(this)
                        })
                    }
                }
            }
            borderpane {
                left {
                    padding = insets(all = 5.0)
                    label(message) {
                        disableWhen { message.isBlank() }
                    }
                }
                right {
                    padding = insets(all = 5.0)
                    button("Ok") {
                        enableWhen { toggleGroup.selectedToggleProperty().isNotNull.and(disableConfirm.not()) }
                        addClass(Styles.ACCENT)
                        isDefaultButton = true
                        action {
                            val selectedToggle = toggleGroup.selectedToggle
                            if (selectedToggle == null) {
                                VripperGuiApplication.APP_INSTANCE.stop()
                            }
                            when ((selectedToggle as RadioButton).id) {
                                "localSession" -> {
                                    coroutineScope.launch {
                                        runLater {
                                            disableConfirm.set(true)
                                            message.set("Starting...")
                                        }
                                        widgetsController.currentSettings.localSession = true
                                        DatabaseManager.connect()
                                        GuiEventBus.publishEvent(GuiEventBus.ChangingSession)
                                        grpcEndpointService.disconnect()
                                        GuiEventBus.publishEvent(GuiEventBus.LocalSession)
                                        runLater {
                                            component.replaceWith(find<AppView>())
                                            close()
                                        }
                                    }
                                }

                                "remoteSession" -> {
                                    coroutineScope.launch {
                                        runLater {
                                            disableConfirm.set(true)
                                            message.set("Connecting...")
                                        }
                                        widgetsController.currentSettings.localSession = false
                                        DatabaseManager.disconnect()
                                        GuiEventBus.publishEvent(GuiEventBus.ChangingSession)
                                        grpcEndpointService.disconnect()
                                        grpcEndpointService.connect(
                                            widgetsController.currentSettings.remoteSessionModel.host,
                                            widgetsController.currentSettings.remoteSessionModel.port
                                        )
                                        repeat(10) {
                                            if (grpcEndpointService.connectionState() == ConnectivityState.READY) {
                                                GuiEventBus.publishEvent(GuiEventBus.RemoteSession)
                                                runLater {
                                                    component.replaceWith(find<AppView>())
                                                    close()
                                                }
                                                cancel()
                                            }
                                            delay(333)
                                        }
                                        runLater {
                                            disableConfirm.set(false)
                                            message.set("Unable to connect")
                                        }
                                    }
                                }

                                else -> VripperGuiApplication.APP_INSTANCE.stop()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUndock() {
        coroutineScope.cancel()
    }
}