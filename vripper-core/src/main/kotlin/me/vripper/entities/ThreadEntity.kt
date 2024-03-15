package me.vripper.entities

data class ThreadEntity(
    val id: Long = -1,
    val title: String,
    val link: String,
    val threadId: Long,
    var total: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThreadEntity

        return threadId == other.threadId
    }

    override fun hashCode(): Int {
        return threadId.hashCode()
    }
}