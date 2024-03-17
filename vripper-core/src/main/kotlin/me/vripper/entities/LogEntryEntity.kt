package me.vripper.entities

import java.time.LocalDateTime

data class LogEntryEntity(
    val id: Long = -1,
    val type: Type,
    val status: Status,
    val time: LocalDateTime = LocalDateTime.now(),
    val message: String,
) {

    enum class Type(val stringValue: String) {
        POST("Post"), THREAD("Thread"), THANKS("Thanks"), METADATA("Metadata"), SCAN("Scan"), DOWNLOAD(
            "Download"
        )
    }

    enum class Status(val stringValue: String) {
        PENDING("Pending"), PROCESSING("Processing"), DONE("Done"), ERROR("Error")
    }
}
