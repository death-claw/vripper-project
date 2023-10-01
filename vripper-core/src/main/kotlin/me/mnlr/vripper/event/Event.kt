package me.mnlr.vripper.event

data class Event<T>(val kind: Kind, val data: T?) {
    enum class Kind {
        POST_UPDATE, POST_REMOVE, IMAGE_UPDATE, METADATA_UPDATE, THREAD_UPDATE, THREAD_REMOVE, LOG_EVENT_UPDATE, LOG_EVENT_REMOVE, VG_USER, DOWNLOAD_STATUS, SETTINGS_UPDATE, THREAD_CLEAR
    }
}