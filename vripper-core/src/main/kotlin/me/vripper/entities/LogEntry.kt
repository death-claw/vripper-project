package me.vripper.entities

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class LogEntry(
    val id: Long = -1,
    val type: Type,
    val status: Status,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") val time: LocalDateTime = LocalDateTime.now(),
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
