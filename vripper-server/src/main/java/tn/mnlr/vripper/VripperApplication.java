package tn.mnlr.vripper;

import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.temporal.ChronoUnit;
import java.util.Collections;

@SpringBootApplication
public class VripperApplication {

    private static final Logger logger = LoggerFactory.getLogger(VripperApplication.class);
    public static final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .handleIf(e -> !(e instanceof InterruptedException))
            .withDelay(1, 3, ChronoUnit.SECONDS)
            .withMaxAttempts(5)
            .abortOn(Collections.singletonList(InterruptedException.class))
            .onFailedAttempt(e -> logger.warn(String.format("#%d tries failed", e.getAttemptCount()), e.getLastFailure()));

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(SpringContext::close));
            SpringApplication.run(VripperApplication.class, args);
        } catch (Exception e) {
            logger.error("Failed to run the application", e);
        }
    }

}

