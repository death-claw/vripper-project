package me.vripper.gui.controller

import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.vripper.gui.model.LogModel
import me.vripper.model.LogEntry
import me.vripper.services.IAppEndpointService
import me.vripper.utilities.LoggerDelegate
import tornadofx.Controller

class LogController : Controller() {
    private val logger by LoggerDelegate()
    lateinit var appEndpointService: IAppEndpointService

    private fun mapper(it: LogEntry): LogModel {
        return LogModel(
            it.sequence,
            it.timestamp,
            it.threadName,
            it.loggerName,
            it.levelString,
            it.formattedMessage,
            it.throwable,
        )
    }

    fun onNewLog() = appEndpointService.onNewLog().map(::mapper).catch {
        logger.error("gRPC error", it)
        currentCoroutineContext().cancel(null)
    }

    suspend fun initLogger() {
        appEndpointService.initLogger()
    }
}