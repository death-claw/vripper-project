package me.mnlr.vripper.gui.listener

import me.mnlr.vripper.gui.clipboard.ClipboardService
import me.mnlr.vripper.listeners.OnStartupListener
import org.koin.core.component.inject

class GuiStartupLister : OnStartupListener() {

    private val clipboardService by inject<ClipboardService>()

    override fun run() {
        super.run()
        clipboardService.init()
    }
}