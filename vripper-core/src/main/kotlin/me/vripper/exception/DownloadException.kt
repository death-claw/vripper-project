package me.vripper.exception

class DownloadException : Exception {
    constructor(message: String?) : super(message)
    constructor(e: Throwable?) : super(e)
}