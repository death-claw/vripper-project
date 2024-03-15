package me.vripper.parser

data class PostItem(
    val threadId: Long,
    val threadTitle: String,
    val postId: Long,
    val number: Int,
    val title: String,
    val imageCount: Int,
    val url: String,
    val hosts: List<Pair<String, Int>>,
    val securityToken: String,
    val forum: String,
    val imageItemList: List<ImageItem>
)
