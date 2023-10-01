package me.vripper.utilities

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val GLOBAL_EXECUTOR: ExecutorService = Executors.newCachedThreadPool()