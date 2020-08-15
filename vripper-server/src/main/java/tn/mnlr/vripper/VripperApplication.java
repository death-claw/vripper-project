package tn.mnlr.vripper;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.temporal.ChronoUnit;

@SpringBootApplication
@Slf4j
public class VripperApplication {

    public static final RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .withDelay(1, 3, ChronoUnit.SECONDS)
            .withMaxAttempts(5)
            .onFailedAttempt(e -> log.warn(String.format("#%d tries failed", e.getAttemptCount()), e.getLastFailure()));

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(SpringContext::close));
            SpringApplication.run(VripperApplication.class, args);
        } catch (Exception e) {
            log.error("Failed to run the application", e);
        }
    }
}

