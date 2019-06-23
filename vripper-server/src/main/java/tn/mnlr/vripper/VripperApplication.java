package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PersistenceService;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import java.awt.*;
import java.io.File;
import java.net.URI;

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
        builder.headless(VripperApplication.headless).run(args);
    }

    @Component
    public class AppCommandRunner implements CommandLineRunner {

        @Autowired
        Environment environment;

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

            openInBrowser();
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

        private void openInBrowser() {

            if (VripperApplication.headless) {
                logger.warn("Headless mode is activated, skipping open in browser");
                return;
            } else {
                logger.info("Not in headless mode, good to open default browser");
            }

            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

                    new Thread(() -> {
                        try {
                            String serverPort = environment.getProperty("server.port", "8080");
                            Desktop.getDesktop().browse(new URI(String.format("http://localhost:%s", serverPort)));
                        } catch (Exception e) {
                            logger.error("Unable to open link in browser", e);
                        }
                    }).start();
                } else {
                    logger.warn("Current platform does not support browser opening");
                }
            } catch (Exception e) {
                logger.error("Unable to verify Desktop compatibility", e);
            }
        }

    }
}


