package me.vripper.exception

class QueueException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, e: Throwable?) : super(message, e)
    constructor(e: Throwable?) : super(e)
}