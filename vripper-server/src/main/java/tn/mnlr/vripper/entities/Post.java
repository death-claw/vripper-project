package tn.mnlr.vripper.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import tn.mnlr.vripper.services.AppStateService;

import java.util.List;
import java.util.Map;
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

    public Post(String title, String url, List<Image> images, Map<String, String> metadata, String postId, AppStateService appStateService) {
        this.title = title;
        this.url = url;
        this.images = images;
        this.metadata = metadata;
        this.postId = postId;
        this.total = images.size();
        this.status = Status.PENDING;
        this.appStateService = appStateService;
        this.appStateService.getCurrentPosts().put(postId, this);
        this.appStateService.getSnapshotPostsState().onNext(this);
    }

    private int total;

    public void increase() {
        done.incrementAndGet();
        this.appStateService.getLivePostsState().onNext(this);
    }

    public void setStatus(Status status) {
        this.status = status;
        if (this.appStateService != null) {
            this.appStateService.getLivePostsState().onNext(this);
        }
    }

    public enum Status {
        PENDING, DOWNLOADING, COMPLETE, ERROR, PARTIAL, STOPPED
    }

    @Override
    public int hashCode() {
        return this.postId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.postId.equals(obj);
    }
}
