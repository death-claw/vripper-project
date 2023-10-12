package me.mnlr.vripper.gui.listener

import me.mnlr.vripper.gui.clipboard.ClipboardService
import me.mnlr.vripper.listeners.OnStartupListener
import me.mnlr.vripper.services.SettingsService
import org.koin.core.component.inject
import java.util.concurrent.CompletableFuture

class GuiStartupLister : OnStartupListener() {

    private val clipboardService by inject<ClipboardService>()
    private val settingsService by inject<SettingsService>()

    override fun run() {
        CompletableFuture.runAsync {
            super.run()
            clipboardService.run(settingsService.settings)
        }
    }
}