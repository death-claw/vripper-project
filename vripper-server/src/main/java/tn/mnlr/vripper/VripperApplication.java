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
import tn.mnlr.vripper.services.PostParser;
import tn.mnlr.vripper.services.VipergirlsAuthService;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class VripperApplication {

    private static final Logger logger = LoggerFactory.getLogger(VripperApplication.class);

    public static final String dataPath = System.getProperty("vripper.datapath", ".") + File.separator + "data.json";

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

    @Component
    public class TestRunner implements CommandLineRunner {

        @Autowired
        private PostParser postParser;

        @Override
        public void run(String... args) throws Exception {

//            long start = System.currentTimeMillis();
//            PostParser.VRThreadParser vrThreadParser = postParser.new VRThreadParser("600408");
//            vrThreadParser.getPostPublishProcessor().subscribe(e -> System.out.println("new Post: " + e.getPostId()));
//            vrThreadParser.parse();
//            System.out.println("Total time: " + (System.currentTimeMillis() - start));
        }
    }
}


