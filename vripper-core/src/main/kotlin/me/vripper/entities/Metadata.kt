package me.vripper.entities

import kotlinx.serialization.Serializable

data class Metadata(val postId: Long, val data: Data) {
    @Serializable
    data class Data(
        val postedBy: String,
        val resolvedNames: List<String>
    )
}
