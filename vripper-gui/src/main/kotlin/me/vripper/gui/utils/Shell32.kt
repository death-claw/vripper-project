package me.vripper.gui.utils

import com.sun.jna.Native
import com.sun.jna.WString
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary

interface Shell32 : ShellAPI, StdCallLibrary {
    fun ShellExecuteW(
        hwnd: WinDef.HWND?,
        lpOperation: WString?,
        lpFile: WString?,
        lpParameters: WString?,
        lpDirectory: WString?,
        nShowCmd: Int
    ): WinDef.HINSTANCE?

    companion object {
        val INSTANCE = Native.load(
            "shell32",
            Shell32::class.java
        ) as Shell32
    }
}