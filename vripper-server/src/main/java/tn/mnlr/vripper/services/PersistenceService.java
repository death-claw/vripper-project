package tn.mnlr.vripper.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.EventListenerBean;
import tn.mnlr.vripper.SpringContext;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);

    private final AppStateExchange appStateExchange;

    @Value("${base.dir}")
    private String baseDir;

    @Getter
    private String dataPath;

    private ObjectMapper om;

    private Disposable subscription;

    @Getter
    private PublishProcessor<Map<String, Post>> processor = PublishProcessor.create();

    @Autowired
    private PersistenceService(AppStateExchange appStateExchange) {
        this.appStateExchange = appStateExchange;
        om = new ObjectMapper();
        om.addMixIn(Image.class, ImagePersistanceMixin.class);
        om.addMixIn(Post.class, PostPersistanceMixin.class);
        subscription = processor
                .onBackpressureBuffer()
                .buffer(1, TimeUnit.SECONDS)
                .filter(e -> !e.isEmpty())
                .doOnNext(e -> this.persist(e.get(0)))
                .subscribe();
    }

    @PostConstruct
    public void init() {
        dataPath = baseDir + File.separator + ".vripper" + File.separator + "data.json";
        File dataFile = new File(dataPath);
        if (!dataFile.exists()) {
            try {
                if (dataFile.getParentFile().mkdirs()) {
                    logger.debug(String.format("%s is created", dataFile.getParentFile().toString()));
                }
                if (!dataFile.getParentFile().isDirectory() || !dataFile.getParentFile().canWrite()) {
                    logger.error(String.format("Unable to write in %s", dataFile.getParent()));
                    SpringContext.close();
                }

                if (dataFile.createNewFile()) {
                    logger.debug("Data file successfully created");
                    try (FileWriter fw = new FileWriter(dataFile)) {
                        fw.write("{}");
                    }
                } else {
                    logger.warn("Data file already exists");
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
        if (EventListenerBean.isInit()) {
            this.persist(appStateExchange.getPosts());
        }
    }

    private void persist(Map<String, Post> currentPosts) {

        try (PrintWriter out = new PrintWriter(dataPath, StandardCharsets.UTF_8)) {
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
            logger.warn(String.format("trying to rename old data file from %s to %s", dataPath, dataPath + "." + timestamp + ".old"));
            try {
                Files.move(new File(dataPath).toPath(), new File(dataPath + "." + timestamp + ".old").toPath());
            } catch (IOException ex) {
                logger.error(String.format("Failed to rename %s to %s", dataPath, dataPath + ".old"));
                SpringContext.close();
            }
        }

        return new HashMap<>();
    }

    public void restore() {

        String jsonContent = null;
        try {
            jsonContent = String.join("", Files.readAllLines(Paths.get(dataPath), StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("data file cannot be read, previous state cannot be restored", e);
            SpringContext.close();
        }

        appStateExchange.restore(read(jsonContent));
    }
}
