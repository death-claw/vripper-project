package tn.mnlr.vripper.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.entities.mixin.persistance.ImagePersistanceMixin;
import tn.mnlr.vripper.entities.mixin.persistance.PostPersistanceMixin;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);

    @Autowired
    private AppStateService stateService;

    private ObjectMapper om;

    @Getter
    private PublishProcessor<Map<String, Post>> processor = PublishProcessor.create();

    private PersistenceService() {
        om = new ObjectMapper();
        om.addMixIn(Image.class, ImagePersistanceMixin.class);
        om.addMixIn(Post.class, PostPersistanceMixin.class);
        processor
                .onBackpressureBuffer()
                .buffer(10, TimeUnit.SECONDS)
                .filter(e -> !e.isEmpty())
                .map(e -> e.get(0))
                .doOnNext(this::persist)
                .subscribe();
    }

    public void persist(Map<String, Post> currentPosts) {
        try(PrintWriter out = new PrintWriter(VripperApplication.dataPath)) {
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
        }

        return new HashMap<>();
    }

    public void restore() {

        String jsonContent;
        try {
            jsonContent = new String(Files.readAllBytes(Paths.get(VripperApplication.dataPath)));
        } catch (Exception e) {
            logger.warn("data file not found, previous state cannot be restored", e);
            return;
        }

        Map<String, Post> read = read(jsonContent);

        stateService.getCurrentPosts().clear();
        stateService.getCurrentPosts().putAll(read);

        stateService.getCurrentImages().clear();
        read.values().stream().flatMap(e -> e.getImages().stream()).forEach(e -> {
            e.setAppStateService(stateService);
            stateService.getCurrentImages().put(e.getUrl(), e);
            stateService.getAllImageState().onNext(e);
        });

        read.values().forEach(e -> {
            e.setAppStateService(stateService);
            stateService.getSnapshotPostsState().onNext(e);
        });
    }
}
