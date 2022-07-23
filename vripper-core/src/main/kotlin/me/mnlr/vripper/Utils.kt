package me.mnlr.vripper

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.formatToString(): String {
    return StringWriter().use { stringWriter ->
        PrintWriter(stringWriter).use { printWriter ->
            this.printStackTrace(printWriter)
            printWriter.flush()
            stringWriter.flush()
            stringWriter.toString()
        }
    }
}

fun Long.formatSI(): String {
    return humanReadableByteCount(this, false)
}

private fun humanReadableByteCount(bytes: Long, si: Boolean): String {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}


fun getExtension(fileName: String): String {
    return if (fileName.contains(".")) fileName.substring(fileName.lastIndexOf(".") + 1) else ""
}

fun getFileNameWithoutExtension(fileName: String): String {
    return if (fileName.contains(".")) fileName.substring(
        0,
        fileName.lastIndexOf(".")
    ) else fileName
}
