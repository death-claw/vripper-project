package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@EnableScheduling
public class DownloadSpeedService {

    private AtomicLong read = new AtomicLong(0);

    @Getter
    private PublishProcessor<Long> readBytesPerSecond = PublishProcessor.create();

    private boolean allowWrite = false;

    public void increase(long read) {
        if (allowWrite) {
            this.read.addAndGet(read);
        }
    }

    @Scheduled(fixedDelay = 1000)
    private void calc() {
        allowWrite = false;
        readBytesPerSecond.onNext(read.getAndSet(0));
        allowWrite = true;
    }
}
