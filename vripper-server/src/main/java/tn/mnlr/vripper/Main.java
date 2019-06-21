package tn.mnlr.vripper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.exception.VripperException;
import tn.mnlr.vripper.services.AppStateService;
import tn.mnlr.vripper.services.PersistenceService;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import java.io.File;

@Component
public class Main implements ApplicationRunner {

    private Logger logger = LoggerFactory.getLogger(Main.class);

    public static final String dataPath = System.getProperty("vripper.datapath", ".") + File.separator + "data.json";

    @Autowired
    private VipergirlsAuthService authService;

    @Autowired
    private PersistenceService persistenceService;

    @Autowired
    private AppStateService stateService;

    @Autowired
    private AppSettings appSettings;

    @Override
    public void run(ApplicationArguments args) {

        persistenceService.restore();
        appSettings.restore();

        try {
            authService.authenticate();
        } catch (VripperException e) {
            logger.error("Cannot authenticate user with ViperGirls", e);
        }
    }
}
