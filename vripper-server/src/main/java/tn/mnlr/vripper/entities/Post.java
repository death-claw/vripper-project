package tn.mnlr.vripper.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppStateService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@ToString
@NoArgsConstructor
public class Post {

    private AppStateService appStateService;

    private Status status;

    private final String type = "post";

    private String postId;

    private String threadTitle;

    private String threadId;

    private String title;

    private String url;

    private List<Image> images;

    private Map<String, Object> metadata;

    private AtomicInteger done = new AtomicInteger(0);

    private int total;

    private Set<String> hosts;

    private boolean removed = false;

    private String forum;

    public Post(String title, String url, List<Image> images, Map<String, Object> metadata, String postId, String threadId, String threadTitle, String forum) {
        this.title = title;
        this.url = url;
        this.images = images;
        this.metadata = metadata;
        this.postId = postId;
        this.forum = forum;
        this.threadId = threadId;
        this.threadTitle = threadTitle;
        if (this.images.contains(null)) {
            System.out.println("Oops");
        }
        this.images.stream().map(e -> {
            if (e == null) {
                System.out.println("Am the cause");
            }
            return e.getHost();
        }).collect(Collectors.toSet());
        this.images.stream().map(e -> e.getHost()).map(Host::getHost).collect(Collectors.toSet());
        this.hosts = this.images.stream().map(e -> e.getHost()).map(Host::getHost).collect(Collectors.toSet());
        total = images.size();
        status = Status.PENDING;

    }

    public void setAppStateService(AppStateService appStateService) {
        this.appStateService = appStateService;
        this.appStateService.getCurrentPosts().put(postId, this);
        this.appStateService.getLivePostsState().onNext(this);
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
        updateNotification();
    }

    public void increase() {
        done.incrementAndGet();
        updateNotification();
    }

    public void setStatus(Status status) {
        this.status = status;
        updateNotification();
    }

    private void updateNotification() {
        if (appStateService != null) {
            appStateService.getLivePostsState().onNext(this);
        }
    }

    public enum Status {
        PENDING, DOWNLOADING, COMPLETE, ERROR, PARTIAL, STOPPED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return postId.equals(post.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId);
    }
}
