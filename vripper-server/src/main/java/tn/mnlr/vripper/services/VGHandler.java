package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.VripperApplication;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Service
public class VGHandler {

    private static final Logger logger = LoggerFactory.getLogger(VGHandler.class);

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private PostParser postParser;

    @Getter
    private LoadingCache<String, List<VRPostState>> cache;

    @PostConstruct
    private void init() {

        CacheLoader<String, List<VRPostState>> loader = new CacheLoader<>() {
            @Override
            public List<VRPostState> load(String threadId) throws Exception {
                VRThreadParser vrThreadParser = postParser.createVRThreadParser(threadId);
                List<VRPostState> posts = new ArrayList<>();
                vrThreadParser.parse();
                vrThreadParser.getPostPublishProcessor()
                        .onBackpressureBuffer()
                        .blockingSubscribe(posts::add);
                return posts;
            }
        };

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(loader);
    }

    public void handle(List<QueuedVGLink> queuedVGLinks) throws Exception {
        for (QueuedVGLink queuedVGLink : queuedVGLinks) {
            if (queuedVGLink.getPostId() != null) {
                postParser.addPost(queuedVGLink.getPostId(), queuedVGLink.getThreadId());
            } else {
                appStateService.getGrabQueue().put(queuedVGLink.getLink(), queuedVGLink);
                appStateService.getLiveGrabQueue().onNext(queuedVGLink);

                Callable<Void> cl = () -> {
                    List<VRPostState> vrPostStates = cache.get(queuedVGLink.getThreadId());
                    logger.debug(String.format("%d found for %s", vrPostStates.size(), queuedVGLink.getLink()));
                    if (vrPostStates.size() == 1) {
                        postParser.addPost(vrPostStates.get(0).getPostId(), vrPostStates.get(0).getThreadId());
                        remove(queuedVGLink.getLink());
                        logger.debug(String.format("threadId %s, postId %s is added automatically for download", queuedVGLink.getThreadId(), queuedVGLink.getPostId()));
                    }

                    return null;
                };

                VripperApplication.commonExecutor.submit(cl);
            }
        }
    }

    public void remove(String url) {
        Optional.ofNullable(appStateService.getGrabQueue().remove(url)).ifPresent(e -> {
            e.setRemoved(true);
            appStateService.getLiveGrabQueue().onNext(e);
        });
    }
}
