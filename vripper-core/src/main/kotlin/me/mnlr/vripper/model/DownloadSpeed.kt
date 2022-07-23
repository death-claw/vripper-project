package me.mnlr.vripper.model

data class DownloadSpeed(val speed: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadSpeed

        if (speed != other.speed) return false

        return true
    }

    override fun hashCode(): Int {
        return speed.hashCode()
    }
}