package me.mnlr.vripper.entities

data class Thread(
    val id: Long? = null,
    val link: String,
    val threadId: String,
    var total: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Thread

        return threadId == other.threadId
    }

    override fun hashCode(): Int {
        return threadId.hashCode()
    }
}