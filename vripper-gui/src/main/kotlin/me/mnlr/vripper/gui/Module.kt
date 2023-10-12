package me.mnlr.vripper.gui

import me.mnlr.vripper.coreModule
import me.mnlr.vripper.gui.clipboard.ClipboardService
import org.koin.dsl.module

val guiModule = module {
    single<ClipboardService> {
        ClipboardService(get(), get())
    }
}

val modules = module {
    includes(coreModule, guiModule)
}