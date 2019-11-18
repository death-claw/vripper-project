package tn.mnlr.vripper;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PersistenceService;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class VripperApplication {

    private static final Logger logger = LoggerFactory.getLogger(VripperApplication.class);
    public static final ExecutorService commonExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) {
        try {
            SpringApplication.run(VripperApplication.class, args);
        } catch (Exception e) {
            logger.error("Failed to run the application", e);
        }
    }

    @Component
    public class AppCommandRunner implements CommandLineRunner {

        @Autowired
        private VipergirlsAuthService authService;

        @Autowired
        private PersistenceService persistenceService;

        @Autowired
        private AppSettingsService appSettingsService;

        @Value("${base.dir}")
        private String baseDir;

        @Getter
        private String dataPath;

        @PostConstruct
        public void init() {
            dataPath = baseDir + File.separator + ".vripper" + File.separator + "data.json";
        }

        @Override
        public void run(String... args) {

            persistenceService.restore();
            appSettingsService.restore();
            registerShutdownHook();

            try {
                authService.authenticate();
            } catch (VripperException e) {
                logger.error("Cannot authenticate user with ViperGirls", e);
            }
        }

        private void registerShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread(SpringContext::close));
        }
    }
}
