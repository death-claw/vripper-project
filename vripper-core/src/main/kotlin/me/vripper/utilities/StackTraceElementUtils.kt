package me.vripper.utilities

import ch.qos.logback.classic.spi.IThrowableProxy

object StackTraceElementUtils {
    fun format(throwable: IThrowableProxy): String {
        val sb = StringBuilder()
        sb.appendLine(throwable.className + ": " + throwable.message)
        stackTraceElementFormat(throwable, sb)
        return sb.toString()

    }

    private fun stackTraceElementFormat(throwable: IThrowableProxy, sb: StringBuilder) {
        throwable.stackTraceElementProxyArray.forEach {
            sb.appendLine("\t" + it.steAsString)
        }

        if (throwable.cause != null) {
            sb.appendLine("Caused by: " + throwable.cause.className + ": " + throwable.cause.message)
            stackTraceElementFormat(throwable.cause, sb)
        }
    }
}