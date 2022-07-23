package me.mnlr.vripper.model

data class ThreadItem(
    val threadId: String,
    val title: String,
    val securityToken: String,
    val forum: String,
    val postItemList: List<PostItem>
)
