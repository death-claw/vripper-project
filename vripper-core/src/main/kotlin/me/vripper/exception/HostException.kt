package me.vripper.exception

class HostException : Exception {
    constructor(e: Throwable?) : super(e)
    constructor(message: String?) : super(message)
    constructor(message: String?, e: Throwable?) : super(message, e)
}