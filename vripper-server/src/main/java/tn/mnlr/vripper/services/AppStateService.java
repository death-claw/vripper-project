package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.q.DownloadJob;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Getter
public class AppStateService {

    private static final Logger logger = LoggerFactory.getLogger(AppStateService.class);

    private Map<String, Image> currentImages = new ConcurrentHashMap<>();

    private Map<String, Post> currentPosts = new ConcurrentHashMap<>();

    private Map<String, AtomicInteger> runningPosts = new ConcurrentHashMap<>();

    private PublishProcessor<Image> liveImageUpdates = PublishProcessor.create();

    private PublishProcessor<Post> livePostsState = PublishProcessor.create();

    @Autowired
    private PersistenceService persistenceService;

    @Autowired
    private AppSettingsService appSettingsService;

    public void onImageUpdate(Image imageState) {

        persistenceService.getProcessor().onNext(currentPosts);

        liveImageUpdates.onNext(imageState);
        if (imageState.isCompleted()) {

            Post postState = currentPosts.get(imageState.getPostId());
            postState.increase();
        }
    }

    public Post getPost(String postId) {
        return currentPosts.get(postId);
    }

    public synchronized void newDownloadJob(DownloadJob downloadJob) {
        String postId = downloadJob.getImage().getPostId();
        checkKeyRunningPosts(postId);
        runningPosts.get(postId).incrementAndGet();
    }

    public synchronized void postDownloadingUpdate(String postId) {
        Post post = currentPosts.get(postId);
        if (!post.getStatus().equals(Post.Status.DOWNLOADING)) {
            post.setStatus(Post.Status.DOWNLOADING);
            livePostsState.onNext(post);
        }
    }

    public void doneDownloadJob(Image image) {
        String postId = image.getPostId();
        int i = runningPosts.get(postId).decrementAndGet();
        Post post = currentPosts.get(postId);
        if(image.getStatus().equals(Image.Status.ERROR)) {
            post.setStatus(Post.Status.PARTIAL);
        }
        if (i == 0) {
            if (post.getImages().stream().map(Image::getStatus).anyMatch(e -> e.equals(Image.Status.ERROR))) {
                post.setStatus(Post.Status.ERROR);
            } else {
                if (!Post.Status.STOPPED.equals(post.getStatus())) {
                    post.setStatus(Post.Status.COMPLETE);
                    if (appSettingsService.isClearCompleted()) {
                        remove(image.getPostId());
                    }
                }
            }
        }
    }

    private synchronized void checkKeyRunningPosts(String key) {
        if (!runningPosts.containsKey(key)) {
            runningPosts.put(key, new AtomicInteger(0));
        }
    }

    public synchronized void remove(String postId) {
        currentPosts.get(postId).setRemoved(true);
        runningPosts.remove(postId);
        currentPosts.remove(postId);
        currentImages.entrySet().removeIf(entry -> entry.getValue().getPostId().equals(postId));
        persistenceService.getProcessor().onNext(currentPosts);
    }

    public synchronized List<String> clearAll() {
        List<String> toRemove = this.currentPosts
                .values()
                .stream()
                .filter(e -> e.getStatus().equals(Post.Status.COMPLETE) && e.getDone().get() >= e.getTotal())
                .map(Post::getPostId)
                .collect(Collectors.toList());
        toRemove.forEach(this::remove);
        return toRemove;
    }

    public synchronized List<String> removeAll() {
        List<String> toRemove = this.currentPosts
                .values()
                .stream()
                .map(Post::getPostId)
                .collect(Collectors.toList());
        toRemove.forEach(this::remove);
        return toRemove;
    }


    @Getter
    public static class CachedThread {

        private Map<String, Post> posts = new ConcurrentHashMap<>();
        private AtomicInteger parsed = new AtomicInteger(0);

        @Setter
        private int total = 0;
    }
}
