package me.vripper.gui

import atlantafx.base.theme.CupertinoDark
import atlantafx.base.theme.CupertinoLight
import javafx.application.Application
import javafx.scene.image.Image
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.stage.WindowEvent
import kotlinx.coroutines.*
import me.vripper.gui.components.views.LoadingView
import me.vripper.gui.controller.WidgetsController
import me.vripper.gui.event.ApplicationInitialized
import me.vripper.listeners.AppLock
import me.vripper.utilities.ApplicationProperties.VRIPPER_DIR
import me.vripper.utilities.DbUtils
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import kotlin.reflect.KClass
import kotlin.system.exitProcess


class VripperGuiApplication : App(
    LoadingView::class
) { //The application class must be a TornadoFX application, and it must have the main view

    private var initialized = false
    private val widgetsController: WidgetsController by inject()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        APP_INSTANCE = this
    }

    override fun start(stage: Stage) {
        Font.loadFont(this.javaClass.getResource("/Inter.ttf")!!.toExternalForm(), 12.0)
        if (widgetsController.currentSettings.darkMode) {
            setUserAgentStylesheet(CupertinoDark().userAgentStylesheet)
        } else {
            setUserAgentStylesheet(CupertinoLight().userAgentStylesheet)
        }
        with(stage) {
            width = widgetsController.currentSettings.width
            height = widgetsController.currentSettings.height
            minWidth = 800.0
            minHeight = 600.0
            icons.addAll(
                listOf(
                    Image("icons/16x16.png"),
                    Image("icons/32x32.png"),
                    Image("icons/48x48.png"),
                    Image("icons/64x64.png"),
                    Image("icons/128x128.png"),
                    Image("icons/256x256.png"),
                    Image("icons/512x512.png"),
                    Image("icons/1024x1024.png")
                )
            )
        }
        coroutineScope.launch {
            while (isActive) {
                if (!stage.isMaximized) {
                    if (widgetsController.currentSettings.width != stage.width || widgetsController.currentSettings.height != stage.height) {
                        widgetsController.currentSettings.width = stage.width
                        widgetsController.currentSettings.height = stage.height
                        widgetsController.update()
                    }
                }
                delay(1_000)
            }
        }
        stage.addEventFilter(WindowEvent.WINDOW_SHOWN) {
            Database.connect("jdbc:h2:file:$VRIPPER_DIR/vripper;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000;")
            DbUtils.update()
            startKoin {
                modules(modules)
            }
            FX.dicontainer =
                object : DIContainer {
                    override fun <T : Any> getInstance(type: KClass<T>): T =
                        GlobalContext.get().get(type)
                }
            fire(ApplicationInitialized)
            initialized = true
        }
        super.start(stage)
    }

    override fun stop() {
        coroutineScope.cancel()
        super.stop()
        exitProcess(0)
    }

    companion object {
        lateinit var APP_INSTANCE: Application
    }
}

fun main(args: Array<String>) {
    AppLock.exclusiveLock()
    Application.launch(VripperGuiApplication::class.java, *args)
}
