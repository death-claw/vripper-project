package me.mnlr.vripper

import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.WindowEvent
import me.mnlr.vripper.event.ApplicationInitialized
import me.mnlr.vripper.gui.Styles
import me.mnlr.vripper.listeners.AppLock
import me.mnlr.vripper.view.LoadingView
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import tornadofx.App
import tornadofx.DIContainer
import tornadofx.FX
import kotlin.reflect.KClass
import kotlin.system.exitProcess


@SpringBootApplication
class VripperGuiApplication : App(
    LoadingView::class, Styles::class
) { //The application class must be a TornadoFX application and it must have the main view

    private lateinit var context: ConfigurableApplicationContext //We are going to set application context here
    private var initialized = false
    override fun start(stage: Stage) {
        with(stage) {
            width = 1200.0
            height = 600.0
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
        stage.addEventFilter(WindowEvent.WINDOW_SHOWN) {
            val contextInitThread = Thread {
                context = SpringApplicationBuilder(this.javaClass).listeners(AppLock())
                    .run() //We start the application context and let Spring Boot to initialize itself
                context.autowireCapableBeanFactory.autowireBean(this) //We ask the context to inject all needed dependencies into the current instence (if needed)

                FX.dicontainer =
                    object : DIContainer { // Here we have to implement an interface for TornadoFX DI support
                        override fun <T : Any> getInstance(type: KClass<T>): T =
                            context.getBean(type.java) // We find dependencies directly in Spring's application context

                        override fun <T : Any> getInstance(type: KClass<T>, name: String): T =
                            context.getBean(name, type.java)
                    }
                fire(ApplicationInitialized)
                initialized = true
            }
            contextInitThread.apply {
                this.name = "Spring init Thread"
            }.start()
        }
        super.start(stage)
    }

    override fun stop() { // On stop, we have to stop spring as well
        super.stop()
        if (initialized) {
            context.close()
        }
        exitProcess(0)
    }
}

fun main(args: Array<String>) {
    Application.launch(VripperGuiApplication::class.java, *args)
}
