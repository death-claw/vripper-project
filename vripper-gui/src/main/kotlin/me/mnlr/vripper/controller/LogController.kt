package me.mnlr.vripper.controller

import me.mnlr.vripper.AppEndpointService
import me.mnlr.vripper.entities.LogEvent
import me.mnlr.vripper.model.LogModel
import me.mnlr.vripper.repositories.LogEventRepository
import tornadofx.Controller
import java.time.format.DateTimeFormatter
import java.util.*

class LogController : Controller() {
    private val logEventRepository: LogEventRepository by di()
    private val appEndpointService: AppEndpointService by di()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")

    fun findAll(): List<LogModel> {
        return logEventRepository.findAll().map(::mapper)
    }

    fun clear() {
        appEndpointService.logClear()
    }

    fun find(id: Long): Optional<LogModel> {
        return logEventRepository.findById(id).map(::mapper)
    }

    private fun mapper(it: LogEvent): LogModel {
        return LogModel(
            it.id!!,
            it.type.stringValue,
            it.status.stringValue,
            it.time.format(dateTimeFormatter),
            it.message
        )
    }
}