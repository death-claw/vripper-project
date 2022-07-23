package me.mnlr.vripper.model

data class GlobalState(
    val running: Int,
    val remaining: Int,
    val error: Int,
    val loggedUser: String,
    val downloadSpeed: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GlobalState

        if (running != other.running) return false
        if (remaining != other.remaining) return false
        if (error != other.error) return false
        if (loggedUser != other.loggedUser) return false
        if (downloadSpeed != other.downloadSpeed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = running
        result = 31 * result + remaining
        result = 31 * result + error
        result = 31 * result + loggedUser.hashCode()
        result = 31 * result + downloadSpeed.hashCode()
        return result
    }
}