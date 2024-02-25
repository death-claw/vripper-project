package me.vripper.gui

import me.vripper.gui.services.ClipboardService
import org.koin.dsl.module

val guiModule = module {
    single<ClipboardService> {
        ClipboardService(get(), get(), get())
    }
}

val modules = module {
    includes(me.vripper.coreModule, guiModule)
}