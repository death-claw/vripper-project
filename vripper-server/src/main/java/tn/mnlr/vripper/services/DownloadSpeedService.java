package tn.mnlr.vripper.services;

import lombok.Getter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tn.mnlr.vripper.listener.EmitHandler;

import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;

@Service
@EnableScheduling
public class DownloadSpeedService {

    private final AtomicLong read = new AtomicLong(0);
    private final Sinks.Many<Long> sink = Sinks.many().multicast().onBackpressureBuffer();
    @Getter
    private long currentValue;
    private boolean allowWrite = false;

    public Flux<Long> getReadBytesPerSecond() {
        return sink.asFlux();
    }

    public void increase(long read) {
        if (allowWrite) {
            this.read.addAndGet(read);
        }
    }

    @Scheduled(fixedDelay = 1000)
    private void calc() {
        allowWrite = false;
        long newValue = read.getAndSet(0);
        if (newValue != currentValue) {
            currentValue = newValue;
            sink.emitNext(currentValue, EmitHandler.RETRY);
        }
        allowWrite = true;
    }

    @PreDestroy
    private void destroy() {
        sink.emitComplete(EmitHandler.RETRY);
    }
}
