package me.mnlr.vripper.entities

class Metadata {
    var postIdRef: Long? = null
    var postId: String? = null
    var postedBy: String? = null
    var resolvedNames = emptyList<String>()

    companion object {
        fun from(metadata: Metadata): Metadata {
            val copy = Metadata()
            copy.postIdRef = metadata.postIdRef
            copy.postId = metadata.postId
            copy.postedBy = metadata.postedBy
            copy.resolvedNames = metadata.resolvedNames
            return copy
        }
    }
}