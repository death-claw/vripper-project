package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.q.DownloadJob;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Getter
public class AppStateService {

    private Map<String, Image> currentImages = new ConcurrentHashMap<>();

    private Map<String, Post> currentPosts = new ConcurrentHashMap<>();

    private Map<String, AtomicInteger> runningPosts = new ConcurrentHashMap<>();

    private PublishProcessor<Image> liveImageUpdates = PublishProcessor.create();

    private PublishProcessor<Post> livePostsState = PublishProcessor.create();

    @Autowired
    private PersistenceService persistenceService;

    public void onImageUpdate(Image imageState) {

        persistenceService.getProcessor().onNext(currentPosts);

        liveImageUpdates.onNext(imageState);
        if (imageState.isCompleted()) {

            Post postState = currentPosts.get(imageState.getPostId());
            postState.increase();
        }
    }

    public Image createImage(String pageUrl, String postId, String postName, Host host) {
        return new Image(pageUrl, postId, postName, host, this);
    }

    public Post createPost(String title, String url, List<Image> images, Map<String, String> metadata, String postId) {
        return new Post(title, url, images, metadata, postId, this);
    }

    public Post getPost(String postId) {
        return currentPosts.get(postId);
    }

    public synchronized void newDownloadJob(DownloadJob downloadJob) {
        String postId = downloadJob.getImage().getPostId();
        checkKeyRunningPosts(postId);
        int i = runningPosts.get(postId).incrementAndGet();
        if (i > 0) {
            Post post = currentPosts.get(postId);
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
            if (post.getImages().stream().map(Image::getStatus).filter(e -> e.equals(Image.Status.ERROR)).count() > 0) {
                post.setStatus(Post.Status.ERROR);
            } else {
                post.setStatus(Post.Status.COMPLETE);
            }
            livePostsState.onNext(post);
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
        Iterator<Map.Entry<String, Image>> iterator = currentImages.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Image> entry = iterator.next();
            if(entry.getValue().getPostId().equals(postId)) {
                iterator.remove();
            }
        }
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
}
