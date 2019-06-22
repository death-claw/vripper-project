package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PersistenceService;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import java.io.File;
import java.util.prefs.Preferences;

@SpringBootApplication
public class VripperApplication {

    private static final Logger logger = LoggerFactory.getLogger(VripperApplication.class);

    public static final String dataPath = System.getProperty("vripper.datapath", ".") + File.separator + "data.json";

	public static void main(String[] args) {
		SpringApplication.run(VripperApplication.class, args);
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

            try {
                authService.authenticate();
            } catch (VripperException e) {
                logger.error("Cannot authenticate user with ViperGirls", e);
            }
        }
    }

}

