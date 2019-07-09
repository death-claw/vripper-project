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

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);

    @Autowired
    private AppStateService stateService;

    private ObjectMapper om;

    private PrintWriter out;

    @Getter
    private PublishProcessor<Map<String, Post>> processor = PublishProcessor.create();

    private PersistenceService() {
        File dataFile = new File(VripperApplication.dataPath);
        if(!dataFile.exists()) {
            try {
                if(dataFile.createNewFile()) {
                    logger.info("Data file successfully created");
                } else {
                    logger.info("Data file already exists");
                }
            } catch (IOException e) {
                logger.error("Unable to create data file", e);
                System.exit(-1);
            }
        }
        om = new ObjectMapper();
        om.addMixIn(Image.class, ImagePersistanceMixin.class);
        om.addMixIn(Post.class, PostPersistanceMixin.class);
        processor
                .onBackpressureLatest()
                .buffer(10, TimeUnit.SECONDS)
                .filter(e -> !e.isEmpty())
                .map(e -> e.get(0))
                .doOnNext(this::persist)
                .subscribe();
    }

    public void persist(Map<String, Post> currentPosts) {
        if(out == null) {
            try {
                out = new PrintWriter(VripperApplication.dataPath, "UTF-8");
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                logger.error(String.format("Failed to create output data file %s", VripperApplication.dataPath));
                System.exit(-1);
            }
        }
        try {
            out.print(om.writeValueAsString(currentPosts));
            out.flush();
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
            jsonContent = Files.readAllLines(Paths.get(VripperApplication.dataPath), Charset.forName("UTF-8")).stream().collect(Collectors.joining());
        } catch (Exception e) {
            logger.warn("data file not found, previous state cannot be restored", e);
            return;
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
