package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AppStateExchange {

    private Map<String, Image> currentImages = new ConcurrentHashMap<>();

    private Map<String, Post> currentPosts = new ConcurrentHashMap<>();

    private Map<String, AtomicInteger> runningPosts = new ConcurrentHashMap<>();

    private Map<String, QueuedVGLink> grabQueue = new ConcurrentHashMap<>();

    private PublishProcessor<QueuedVGLink> liveGrabQueue = PublishProcessor.create();

    private PublishProcessor<Image> liveImageUpdates = PublishProcessor.create();

    private PublishProcessor<Post> livePostsState = PublishProcessor.create();

    public Map<String, AtomicInteger> running() {
        return runningPosts;
    }

    public Map<String, Post> getPosts() {
        return this.currentPosts;
    }

    public Post getPost(String postId) {
        return currentPosts.get(postId);
    }

    public Map<String, Image> getImages() {
        return currentImages;
    }

    public PublishProcessor<Image> liveImage() {
        return liveImageUpdates;
    }

    public PublishProcessor<Post> livePost() {
        return livePostsState;
    }

    public Map<String, QueuedVGLink> getQueue() {
        return grabQueue;
    }

    public PublishProcessor<QueuedVGLink> liveQueue() {
        return liveGrabQueue;
    }

    public synchronized void restore(Map<String, Post> read) {
        currentPosts.clear();
        currentPosts.putAll(read);
        currentPosts.values().forEach(p -> {
            if (Arrays.asList(Post.Status.DOWNLOADING, Post.Status.PARTIAL, Post.Status.PENDING).contains(p.getStatus())) {
                p.setStatus(Post.Status.STOPPED);
            }
        });
        currentImages.clear();
        read.values().stream().flatMap(e -> e.getImages().stream()).forEach(e -> currentImages.put(e.getUrl(), e));
    }
}
