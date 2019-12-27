package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class VripperApplication {

    private static final Logger logger = LoggerFactory.getLogger(VripperApplication.class);
    public static final ExecutorService commonExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(SpringContext::close));
            SpringApplication.run(VripperApplication.class, args);
        } catch (Exception e) {
            logger.error("Failed to run the application", e);
        }
    }
}

