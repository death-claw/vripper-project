package me.vripper.gui.controller

import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.vripper.delegate.LoggerDelegate
import me.vripper.gui.model.LogModel
import me.vripper.model.LogEntry
import me.vripper.services.IAppEndpointService
import tornadofx.Controller
import java.time.format.DateTimeFormatter

class LogController : Controller() {
    private val logger by LoggerDelegate()
    lateinit var appEndpointService: IAppEndpointService
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun findAll(): List<LogModel> {
        return appEndpointService.findAllLogs().map(::mapper)
    }

    suspend fun clear() {
        appEndpointService.logClear()
    }

    private fun mapper(it: LogEntry): LogModel {
        return LogModel(
            it.id, it.type.stringValue, it.status.stringValue, it.time.format(dateTimeFormatter), it.message
        )
    }

    fun onNewLog() = appEndpointService.onNewLog().map(::mapper).catch {
        logger.error("gRPC error", it)
        currentCoroutineContext().cancel(null)
    }

    fun onUpdateLog() = appEndpointService.onUpdateLog().catch {
        logger.error("gRPC error", it)
        currentCoroutineContext().cancel(null)
    }

    fun onDeleteLogs() = appEndpointService.onDeleteLogs().catch {
        logger.error("gRPC error", it)
        currentCoroutineContext().cancel(null)
    }
}