package me.vripper.utilities

import kotlinx.coroutines.sync.Semaphore

object RequestLimit {
    val semaphore: Semaphore = Semaphore(6)
}
