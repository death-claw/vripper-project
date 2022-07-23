package me.mnlr.vripper.services

import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class ThreadPoolService {
    val generalExecutor: ExecutorService = Executors.newFixedThreadPool(4)
    fun destroy() {
        generalExecutor.shutdown()
        if (!generalExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            generalExecutor.shutdownNow()
        }
    }
}