package tn.mnlr.vripper.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tn.mnlr.vripper.services.AppStateService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ToString
@NoArgsConstructor
public class Post {

    @Setter
    private AppStateService appStateService;

    private Status status;

    private final String type = "post";

    private String postId;

    private String title;

    private String url;

    private List<Image> images;

    private Map<String, String> metadata;

    private AtomicInteger done = new AtomicInteger(0);

    private int total;

    private boolean removed = false;

    public Post(String title, String url, List<Image> images, Map<String, String> metadata, String postId, AppStateService appStateService) {
        this.title = title;
        this.url = url;
        this.images = images;
        this.metadata = metadata;
        this.postId = postId;
        this.appStateService = appStateService;
        total = images.size();
        status = Status.PENDING;
        appStateService.getCurrentPosts().put(postId, this);
        appStateService.getLivePostsState().onNext(this);
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
        appStateService.getLivePostsState().onNext(this);
    }

    public void increase() {
        done.incrementAndGet();
        appStateService.getLivePostsState().onNext(this);
    }

    public void setStatus(Status status) {
        this.status = status;
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
