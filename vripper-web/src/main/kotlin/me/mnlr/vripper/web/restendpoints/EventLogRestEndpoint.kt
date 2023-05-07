package me.mnlr.vripper.web.restendpoints

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import me.mnlr.vripper.repositories.LogEventRepository
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping("/api")
class EventLogRestEndpoint(private val logEventRepository: LogEventRepository) {
    @GetMapping("/events/clear")
    @ResponseStatus(value = HttpStatus.OK)
    fun clear() {
        logEventRepository.deleteAll()
    }
}