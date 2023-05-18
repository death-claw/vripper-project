package me.mnlr.vripper.services

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

@Service
class AsyncTaskRunnerService {
    val sink = Sinks.many().unicast().onBackpressureBuffer<Runnable>()

    @PostConstruct
    fun init() {
        sink.asFlux().subscribeOn(Schedulers.parallel()).subscribe {
            it.run()
        }
    }
}