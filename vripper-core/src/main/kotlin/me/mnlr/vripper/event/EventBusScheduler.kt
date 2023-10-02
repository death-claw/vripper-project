package me.mnlr.vripper.event

import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object EventBusScheduler {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    val scheduler: Scheduler = Schedulers.fromExecutor(executor)
}