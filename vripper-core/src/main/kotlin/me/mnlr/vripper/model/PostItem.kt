package me.mnlr.vripper.model

import me.mnlr.vripper.host.Host

data class PostItem(
    val threadId: String,
    val threadTitle: String,
    val postId: String,
    val number: Int,
    val title: String,
    val imageCount: Int,
    val url: String,
    val hosts: Map<Host, Int>,
    val securityToken: String,
    val forum: String,
    val imageItemList: List<ImageItem>
)