package me.vripper.web.restendpoints

import me.vripper.services.DataTransaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class EventLogRestEndpoint : KoinComponent {

    private val dataTransaction: DataTransaction by inject()
    @GetMapping("/events/clear")
    @ResponseStatus(value = HttpStatus.OK)
    fun clear() {
        dataTransaction.deleteAllLogs()
    }
}