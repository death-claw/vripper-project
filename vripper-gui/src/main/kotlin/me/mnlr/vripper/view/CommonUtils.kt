package me.mnlr.vripper.view

import com.sun.jna.WString
import me.mnlr.vripper.VripperGuiApplication
import me.mnlr.vripper.utils.Shell32

fun openFileDirectory(path: String) {
    val os = System.getProperty("os.name")
    if(os.contains("Windows")) {
        Shell32.INSTANCE.ShellExecuteW(null, WString("open"), WString(path), null, null, 1)
    } else if(os.contains("Linux")) {
        Runtime.getRuntime().exec("xdg-open $path")
    } else if(os.contains("Mac")) {
        Runtime.getRuntime().exec("open -R $path")
    }
}

fun openLink(path: String) {
    VripperGuiApplication.APP_INSTANCE.hostServices.showDocument(path)
}