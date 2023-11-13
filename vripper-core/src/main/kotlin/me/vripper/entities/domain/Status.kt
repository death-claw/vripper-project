package me.vripper.entities.domain

enum class Status(val stringValue: String) {
    PENDING("Pending"), DOWNLOADING("Downloading"), FINISHED("Finished"), ERROR("Error"), STOPPED("Stopped")
}