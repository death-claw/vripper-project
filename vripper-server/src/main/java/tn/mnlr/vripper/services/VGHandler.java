package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.services.post.CachedPost;
import tn.mnlr.vripper.services.post.PostService;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VGHandler {

    private final DataService dataService;
    private final PostService postService;
    private final CommonExecutor commonExecutor;

    @Getter
    private final LoadingCache<Queued, List<CachedPost>> cache;

    @Autowired
    public VGHandler(DataService dataService, PostService postService, CommonExecutor commonExecutor) {
        this.dataService = dataService;
        this.postService = postService;
        this.commonExecutor = commonExecutor;

        CacheLoader<Queued, List<CachedPost>> loader = new CacheLoader<>() {
            @Override
            public List<CachedPost> load(@NonNull Queued queuedVGLink) throws Exception {
                VRThreadParser vrThreadParser = new VRThreadParser(queuedVGLink);
                return vrThreadParser.parse();
            }
        };

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(loader);
    }

    public void handle(List<Queued> queuedList) throws Exception {
        for (Queued queued : queuedList) {
            if (queued.getPostId() != null) {
                postService.addPost(queued.getPostId(), queued.getThreadId());
            } else {
                Runnable runnable = () -> {
                    List<CachedPost> cachedPosts;
                    try {
                        cachedPosts = cache.get(queued);
                    } catch (ExecutionException e) {
                        log.error(String.format("Failed to add post with thread id %s, postId %s", queued.getThreadId(), queued.getPostId()), e);
                        return;
                    }
                    queued.setTotal(cachedPosts.size());
                    queued.done();
                    log.debug(String.format("%d found for %s", cachedPosts.size(), queued.getLink()));
                    if (cachedPosts.size() == 1) {
                        try {
                            postService.addPost(cachedPosts.get(0).getPostId(), cachedPosts.get(0).getThreadId());
                        } catch (PostParseException e) {
                            log.error(String.format("Failed to add post with postId %s", cachedPosts.get(0).getPostId()), e);
                            return;
                        }
                        log.debug(String.format("threadId %s, postId %s is added automatically for download", queued.getThreadId(), queued.getPostId()));
                    } else {
                        dataService.newQueueLink(queued);
                    }
                };
                commonExecutor.getGeneralExecutor().submit(runnable);
            }
        }
    }

    public void remove(String threadId) {
        dataService.removeQueueLink(threadId);
    }
}
