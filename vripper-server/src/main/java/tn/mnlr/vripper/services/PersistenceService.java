package tn.mnlr.vripper.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.entities.mixin.persistance.ImagePersistanceMixin;
import tn.mnlr.vripper.entities.mixin.persistance.PostPersistanceMixin;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);

    @Autowired
    private AppStateService stateService;

    @Autowired
    private VripperApplication.AppCommandRunner appCommandRunner;

    private ObjectMapper om;

    private Disposable subscription;

    @Getter
    private PublishProcessor<Map<String, Post>> processor = PublishProcessor.create();

    private PersistenceService() {
        om = new ObjectMapper();
        om.addMixIn(Image.class, ImagePersistanceMixin.class);
        om.addMixIn(Post.class, PostPersistanceMixin.class);
        subscription = processor
                .onBackpressureLatest()
                .buffer(10, TimeUnit.SECONDS)
                .filter(e -> !e.isEmpty())
                .map(e -> e.get(0))
                .doOnNext(this::persist)
                .subscribe();
    }

    @PostConstruct
    public void init() {
        File dataFile = new File(appCommandRunner.getDataPath());
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                if (!dataFile.getParentFile().isDirectory() || !dataFile.getParentFile().canWrite()) {
                    logger.error(String.format("Unable to write in %s", dataFile.getParent()));
                    SpringContext.close();
                }

                if (dataFile.createNewFile()) {
                    logger.info("Data file successfully created");
                    try (FileWriter fw = new FileWriter(dataFile)) {
                        fw.write("{}");
                    }
                } else {
                    logger.info("Data file already exists");
                }
            } catch (IOException e) {
                logger.error("Unable to create data file", e);
                SpringContext.close();
            }
        }
    }

    @PreDestroy
    public void preDestroy() {
        this.subscription.dispose();
        logger.info(String.format("Destroying %s", PersistenceService.class.getSimpleName()));
        logger.info("Persisting data before destroying");
        this.persist(stateService.getCurrentPosts());
    }

    public void persist(Map<String, Post> currentPosts) {

        try (PrintWriter out = new PrintWriter(appCommandRunner.getDataPath(), "UTF-8")) {
            out.print(om.writeValueAsString(currentPosts));
        } catch (IOException e) {
            logger.error("Failed to persist app state", e);
        }
    }

    private Map<String, Post> read(String content) {

        try {
            return om.readValue(content, om.getTypeFactory().constructMapType(HashMap.class, String.class, Post.class));
        } catch (IOException e) {
            logger.error("Failed to read app state", e);
            long timestamp = new Date().getTime();
            logger.warn(String.format("trying to rename old data file from %s to %s", appCommandRunner.getDataPath(), appCommandRunner.getDataPath() + "." + timestamp + ".old"));
            try {
                Files.move(new File(appCommandRunner.getDataPath()).toPath(), new File(appCommandRunner.getDataPath() + "." + timestamp + ".old").toPath());
            } catch (IOException ex) {
                logger.error(String.format("Failed to rename %s to %s", appCommandRunner.getDataPath(), appCommandRunner.getDataPath() + ".old"));
                SpringContext.close();
            }
        }

        return new HashMap<>();
    }

    public void restore() {

        String jsonContent = null;
        try {
            jsonContent = Files.readAllLines(Paths.get(appCommandRunner.getDataPath()), StandardCharsets.UTF_8).stream().collect(Collectors.joining());
        } catch (Exception e) {
            logger.error("data file cannot be read, previous state cannot be restored", e);
            SpringContext.close();
        }

        Map<String, Post> read = read(jsonContent);

        stateService.getCurrentPosts().clear();
        stateService.getCurrentPosts().putAll(read);
        stateService.getCurrentPosts().values().forEach(p -> {
            if(Arrays.asList(Post.Status.DOWNLOADING, Post.Status.PARTIAL).contains(p.getStatus())) {
                p.setStatus(Post.Status.STOPPED);
            }
        });

        stateService.getCurrentImages().clear();
        read.values().stream().flatMap(e -> e.getImages().stream()).forEach(e -> {
            e.setAppStateService(stateService);
            stateService.getCurrentImages().put(e.getUrl(), e);
        });

        read.values().forEach(e -> {
            e.setAppStateService(stateService);
        });
    }
}
