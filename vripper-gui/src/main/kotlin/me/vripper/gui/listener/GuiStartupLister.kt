package me.vripper.gui.listener

import me.vripper.listeners.OnStartupListener
import me.vripper.utilities.GLOBAL_EXECUTOR
import java.util.concurrent.CompletableFuture

class GuiStartupLister : OnStartupListener() {

    override fun run() {
        CompletableFuture.runAsync({
            super.run()
        }, GLOBAL_EXECUTOR)
    }
}