package me.vripper.gui.utils

import kotlinx.coroutines.Job

object ActiveUICoroutines {
    val posts: MutableList<Job> = mutableListOf()
    val actionBar: MutableList<Job> = mutableListOf()
    val images: MutableList<Job> = mutableListOf()
    val logs: MutableList<Job> = mutableListOf()
    val menuBar: MutableList<Job> = mutableListOf()
    val postInfo: MutableList<Job> = mutableListOf()
    val statusBar: MutableList<Job> = mutableListOf()
    val threads: MutableList<Job> = mutableListOf()

    fun all() = listOf(
        posts,
        actionBar,
        images,
        logs,
        menuBar,
        postInfo,
        statusBar,
        threads
    ).flatten()
}