package me.mnlr.vripper.exception

class VripperException : Exception {
    constructor(message: String?) : super(message)
    constructor(e: Throwable?) : super(e)
}