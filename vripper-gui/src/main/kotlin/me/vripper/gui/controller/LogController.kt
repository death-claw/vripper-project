package me.vripper.gui.controller

import me.vripper.entities.LogEntry
import me.vripper.gui.model.LogModel
import me.vripper.services.AppEndpointService
import me.vripper.services.DataTransaction
import tornadofx.Controller
import java.time.format.DateTimeFormatter

class LogController : Controller() {
    private val appEndpointService: AppEndpointService by di()
    private val dataTransaction: DataTransaction by di()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun findAll(): List<LogModel> {
        return dataTransaction.findAllLogs().map(::mapper)
    }

    fun clear() {
        appEndpointService.logClear()
    }

    fun mapper(it: LogEntry): LogModel {
        return LogModel(
            it.id,
            it.type.stringValue,
            it.status.stringValue,
            it.time.format(dateTimeFormatter),
            it.message
        )
    }
}