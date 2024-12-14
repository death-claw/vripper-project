package me.vripper.utilities

import kotlinx.coroutines.sync.Semaphore

internal object RequestLimit {
    val semaphore: Semaphore = Semaphore(2)
}
