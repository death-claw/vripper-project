package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Service
public class VGHandler {

    private static final Logger logger = LoggerFactory.getLogger(VGHandler.class);

    private final AppStateService appStateService;
    private final PostParser postParser;
    private final CommonExecutor commonExecutor;

    @Getter
    private final LoadingCache<QueuedVGLink, List<VRPostState>> cache;

    @Autowired
    public VGHandler(AppStateService appStateService, PostParser postParser, CommonExecutor commonExecutor) {
        this.appStateService = appStateService;
        this.postParser = postParser;
        this.commonExecutor = commonExecutor;

        CacheLoader<QueuedVGLink, List<VRPostState>> loader = new CacheLoader<>() {
            @Override
            public List<VRPostState> load(@NonNull QueuedVGLink queuedVGLink) throws Exception {
                VRThreadParser vrThreadParser = postParser.createVRThreadParser(queuedVGLink);
                return vrThreadParser.parse();
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
                Callable<Void> cl = () -> {
                    List<VRPostState> vrPostStates = cache.get(queuedVGLink);
                    queuedVGLink.setCount(vrPostStates.size());
                    queuedVGLink.done();
                    logger.debug(String.format("%d found for %s", vrPostStates.size(), queuedVGLink.getLink()));
                    if (vrPostStates.size() == 1) {
                        queuedVGLink.remove();
                        postParser.addPost(vrPostStates.get(0).getPostId(), vrPostStates.get(0).getThreadId());
                        logger.debug(String.format("threadId %s, postId %s is added automatically for download", queuedVGLink.getThreadId(), queuedVGLink.getPostId()));
                    } else {
                        appStateService.newQueueLink(queuedVGLink);
                    }
                    return null;
                };
                commonExecutor.getGeneralExecutor().submit(cl);
            }
        }
    }

    public void remove(String threadId) {
        appStateService.removeQueueLink(threadId);
    }
}
