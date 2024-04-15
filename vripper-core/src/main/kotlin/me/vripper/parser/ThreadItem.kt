package me.vripper.parser

data class ThreadItem(
    val threadId: Long,
    val title: String,
    val securityToken: String,
    val forum: String,
    val postItemList: List<PostItem>,
    val error: String
)
