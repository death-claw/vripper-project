package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.services.PersistenceService;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import java.io.File;

@SpringBootApplication
public class VripperApplication {

    public static final String dataPath = System.getProperty("vripper.datapath", ".") + File.separator + "data.json";

	public static void main(String[] args) {
		SpringApplication.run(VripperApplication.class, args);
	}

    @Component
    public class AppCommandRunner implements CommandLineRunner {

        private final Logger logger = LoggerFactory.getLogger(AppCommandRunner.class);

        @Autowired
        private VipergirlsAuthService authService;

        @Autowired
        private PersistenceService persistenceService;

        @Autowired
        private AppSettings appSettings;

        @Override
        public void run(String... args) {
            persistenceService.restore();
            appSettings.restore();

            try {
                authService.authenticate();
            } catch (VripperException e) {
                logger.error("Cannot authenticate user with ViperGirls", e);
            }
        }
    }

}

