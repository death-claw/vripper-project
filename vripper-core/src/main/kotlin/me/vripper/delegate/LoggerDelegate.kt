package me.vripper.delegate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LoggerDelegate : ReadOnlyProperty<Any?, Logger> {
    private var logger: Logger? = null
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Logger {
        if (logger == null) {
            logger = LoggerFactory.getLogger(thisRef!!.javaClass)
        }
        return logger!!
    }
}
