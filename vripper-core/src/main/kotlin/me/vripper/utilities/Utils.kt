package me.vripper.utilities

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val executorService: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()