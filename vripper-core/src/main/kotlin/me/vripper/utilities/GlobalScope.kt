package me.vripper.utilities

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val log by LoggerDelegate()

val errorHandler = CoroutineExceptionHandler { _, exception ->
    log.error("Unexpected error", exception)
}

val GlobalScopeCoroutine = CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler)