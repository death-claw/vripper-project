package tn.mnlr.vripper.services;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.q.DownloadJob;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AppStateService {

    private final PersistenceService persistenceService;
    private final AppSettingsService appSettingsService;
    private final AppStateExchange appStateExchange;

    @Autowired
    public AppStateService(PersistenceService persistenceService, AppSettingsService appSettingsService, AppStateExchange appStateExchange) {
        this.appStateExchange = appStateExchange;
        this.persistenceService = persistenceService;
        this.appSettingsService = appSettingsService;
    }

    public synchronized void newQueueLink(QueuedVGLink queuedVGLink) {
        appStateExchange.getQueue().put(queuedVGLink.getThreadId(), queuedVGLink);
        appStateExchange.liveQueue().onNext(queuedVGLink);
    }

    public synchronized void queueLinkUpdated(QueuedVGLink queuedVGLink) {
        appStateExchange.liveQueue().onNext(queuedVGLink);
    }

    public synchronized void removeQueueLink(String threadId) {
        Optional.ofNullable(appStateExchange.getQueue().remove(threadId)).ifPresent(QueuedVGLink::remove);
    }

    public synchronized boolean newPost(@NonNull Post post) {
        if (appStateExchange.getPosts().containsKey(post.getPostId())) {
            return false;
        } else {
            appStateExchange.getPosts().put(post.getPostId(), post);
            appStateExchange.livePost().onNext(post);
            persistenceService.getProcessor().onNext(appStateExchange.getPosts());
            return true;
        }
    }

    public synchronized void postUpdated(@NonNull Post post) {
        appStateExchange.livePost().onNext(post);
        persistenceService.getProcessor().onNext(appStateExchange.getPosts());
    }

    public boolean newImage(Image image) {
        if (appStateExchange.getImages().containsKey(image.getUrl())) {
            return false;
        } else {
            appStateExchange.getImages().put(image.getUrl(), image);
            appStateExchange.liveImage().onNext(image);
            persistenceService.getProcessor().onNext(appStateExchange.getPosts());
            return true;
        }
    }

    public synchronized void imageUpdated(Image image) {

        persistenceService.getProcessor().onNext(appStateExchange.getPosts());
        appStateExchange.liveImage().onNext(image);

        if (image.isCompleted()) {
            Post postState = appStateExchange.getPosts().get(image.getPostId());
            postState.increase();
        }
    }

    public synchronized void newDownloadJob(@NonNull DownloadJob downloadJob) {
        String postId = downloadJob.getImage().getPostId();
        checkKeyRunningPosts(postId);
        appStateExchange.getRunningCount(postId).incrementAndGet();
    }

    public synchronized void postDownloadingUpdate(@NonNull String postId) {
        Post post = appStateExchange.getPosts().get(postId);
        if (!post.getStatus().equals(Post.Status.DOWNLOADING) && !post.getStatus().equals(Post.Status.PARTIAL)) {
            post.setStatus(Post.Status.DOWNLOADING);
            appStateExchange.livePost().onNext(post);
        }
    }

    public synchronized void doneDownloadJob(@NonNull Image image) {
        String postId = image.getPostId();
        int i = appStateExchange.getRunningCount(postId).decrementAndGet();
        Post post = appStateExchange.getPosts().get(postId);
        if (image.getStatus().equals(Image.Status.ERROR)) {
            post.setStatus(Post.Status.PARTIAL);
        }
        if (i == 0) {
            if (post.getImages().stream().map(Image::getStatus).anyMatch(e -> e.equals(Image.Status.ERROR))) {
                post.setStatus(Post.Status.ERROR);
            } else {
                if (!Post.Status.STOPPED.equals(post.getStatus())) {
                    post.setStatus(Post.Status.COMPLETE);
                    if (appSettingsService.getSettings().getClearCompleted()) {
                        remove(image.getPostId());
                    }
                }
            }
        }
    }

    public boolean isRunning(String postId) {
        AtomicInteger runningCount = appStateExchange.getRunningCount(postId);
        return runningCount != null && runningCount.get() > 0;
    }

    public synchronized List<String> clearAll() {
        return this.appStateExchange.getPosts()
                .values()
                .stream()
                .filter(e -> e.getStatus().equals(Post.Status.COMPLETE) && e.getDone().get() >= e.getTotal())
                .map(Post::getPostId)
                .peek(this::remove)
                .collect(Collectors.toList());
    }

    public synchronized List<String> removeAll(List<String> postIds) {
        if (postIds != null && !postIds.isEmpty()) {
            postIds.forEach(this::remove);
            return postIds;
        } else {
            return this.appStateExchange.getPosts()
                    .values()
                    .stream()
                    .map(Post::getPostId)
                    .peek(this::remove)
                    .collect(Collectors.toList());
        }
    }

    private void checkKeyRunningPosts(@NonNull String key) {
        if (!appStateExchange.running().containsKey(key)) {
            appStateExchange.running().put(key, new AtomicInteger(0));
        }
    }

    private void remove(@NonNull String postId) {
        final Post post = appStateExchange.getPosts().get(postId);
        if (post == null) {
            return;
        }
        post.setRemoved(true);
        appStateExchange.running().remove(postId);
        appStateExchange.getPosts().remove(postId);
        appStateExchange.getImages().entrySet().removeIf(entry -> entry.getValue().getPostId().equals(postId));
        persistenceService.getProcessor().onNext(appStateExchange.getPosts());
    }

    @Getter
    public static class CachedThread {

        private Map<String, Post> posts = new ConcurrentHashMap<>();
        private AtomicInteger parsed = new AtomicInteger(0);

        @Setter
        private int total = 0;
    }
}
