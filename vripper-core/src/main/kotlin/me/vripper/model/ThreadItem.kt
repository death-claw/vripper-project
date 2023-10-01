package me.vripper.model

data class ThreadItem(
    val threadId: Long,
    val title: String,
    val securityToken: String,
    val forum: String,
    val postItemList: List<PostItem>
)
