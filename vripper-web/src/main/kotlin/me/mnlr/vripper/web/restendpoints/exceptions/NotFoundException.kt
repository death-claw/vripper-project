package me.mnlr.vripper.web.restendpoints.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND)
class NotFoundException(message: String?) : RuntimeException(message)
