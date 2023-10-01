package me.vripper.web.restendpoints.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
class ServerErrorException(message: String?) : RuntimeException(message)
