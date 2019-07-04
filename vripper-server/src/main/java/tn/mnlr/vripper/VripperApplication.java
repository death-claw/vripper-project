package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.services.*;

import java.io.File;

@SpringBootApplication
public class VripperApplication {

    private static final Logger logger = LoggerFactory.getLogger(VripperApplication.class);

    public static final String dataPath = System.getProperty("vripper.datapath", ".") + File.separator + "data.json";

    public static boolean headless = false;

    public static void main(String[] args) {

        String headless = System.getProperty("java.awt.headless");
        if (headless != null && headless.trim().toLowerCase().equals("true")) {
            VripperApplication.headless = true;
        }

        SpringApplicationBuilder builder = new SpringApplicationBuilder(VripperApplication.class);
        try {
            builder.headless(VripperApplication.headless).run(args);
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


