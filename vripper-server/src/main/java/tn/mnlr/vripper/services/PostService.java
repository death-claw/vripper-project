package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.download.PendingQueue;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.services.domain.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PostService {

    private final SettingsService settingsService;
    private final PendingQueue pendingQueue;
    private final DataService dataService;
    private final ThreadPoolService threadPoolService;
    private final Map<String, Future<?>> fetchingMetadata = new ConcurrentHashMap<>();
    private final VGAuthService VGAuthService;

    @Getter
    private final LoadingCache<Queued, List<MultiPostItem>> cache;

    @Autowired
    public PostService(SettingsService settingsService, PendingQueue pendingQueue, DataService dataService, ThreadPoolService threadPoolService, VGAuthService VGAuthService) {
        this.settingsService = settingsService;
        this.pendingQueue = pendingQueue;
        this.dataService = dataService;
        this.threadPoolService = threadPoolService;
        this.VGAuthService = VGAuthService;

        CacheLoader<Queued, List<MultiPostItem>> loader = new CacheLoader<>() {
            @Override
            public List<MultiPostItem> load(@NonNull Queued multiPostItem) throws Exception {
                ApiThreadParser apiThreadParser = new ApiThreadParser(multiPostItem);
                return apiThreadParser.parse();
            }
        };

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(loader);
    }

    public void addPost(String postId, String threadId) throws PostParseException {

        if (dataService.exists(postId)) {
            log.warn(String.format("skipping %s, already loaded", postId));
            return;
        }

        ApiPostParser apiPostParser = new ApiPostParser(threadId, postId);
        ApiPost apiPost = apiPostParser.parse();
        if (apiPost.getPost().isEmpty()) {
            throw new PostParseException(String.format("parsing failed for thread %s, post %s", threadId, postId));
        }

        Post post = apiPost.getPost().get();
        Set<Image> images = apiPost.getImages();

        dataService.newPost(post, images);

        // Metadata thread
        fetchingMetadata.put(post.getPostId(), threadPoolService.getGeneralExecutor().submit(new MetadataRunnable(post)));

        if (settingsService.getSettings().getAutoStart()) {
            log.debug("Auto start downloads option is enabled");
            post.setStatus(Status.PENDING);
            try {
                pendingQueue.enqueue(post, images);
            } catch (InterruptedException e) {
                log.warn("Interruption was caught");
                Thread.currentThread().interrupt();
                return;
            }
            log.debug(String.format("Done enqueuing jobs for %s", post.getUrl()));
        } else {
            post.setStatus(Status.STOPPED);
            log.debug("Auto start downloads option is disabled");
        }
        if (!settingsService.getSettings().getLeaveThanksOnStart()) {
            VGAuthService.leaveThanks(post);
        }
        dataService.updatePostStatus(post.getStatus(), post.getId());
    }

    public void stopFetchingMetadata(Post post) {
        this.fetchingMetadata.forEach((k, v) -> {
            if (k.equals(post.getPostId())) {
                v.cancel(true);
            }
        });
        fetchingMetadata.remove(post.getPostId());
    }

    public void processMultiPost(List<Queued> queuedList) throws Exception {
        for (Queued queued : queuedList) {
            if (queued.getPostId() != null) {
                addPost(queued.getPostId(), queued.getThreadId());
            } else {
                threadPoolService.getGeneralExecutor().submit(() -> this.multiPost(queued));
            }
        }
    }

    private void multiPost(Queued queued) {

        List<MultiPostItem> multiPostItems;
        try {
            multiPostItems = cache.get(queued);
        } catch (ExecutionException e) {
            log.error(String.format("Failed to add post with thread id %s, postId %s", queued.getThreadId(), queued.getPostId()), e);
            return;
        }
        queued.setTotal(multiPostItems.size());
        queued.done();
        log.debug(String.format("%d found for %s", multiPostItems.size(), queued.getLink()));
        if (multiPostItems.size() == 1) {
            try {
                addPost(multiPostItems.get(0).getPostId(), multiPostItems.get(0).getThreadId());
            } catch (PostParseException e) {
                log.error(String.format("Failed to add post with postId %s", multiPostItems.get(0).getPostId()), e);
                return;
            }
            log.debug(String.format("threadId %s, postId %s is added automatically for download", queued.getThreadId(), queued.getPostId()));
        } else {
            if (dataService.findQueuedByThreadId(queued.getThreadId()).isEmpty()) {
                dataService.newQueueLink(queued);
            } else {
                log.info(String.format("Thread with id = %s is already loaded", queued.getThreadId()));
            }
        }

    }

    public void remove(String threadId) {
        dataService.removeQueueLink(threadId);
    }
}
