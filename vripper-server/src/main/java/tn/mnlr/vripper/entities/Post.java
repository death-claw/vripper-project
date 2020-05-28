package tn.mnlr.vripper.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.exception.PostParseException;
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
public class Post {

    public enum METADATA {
        PREVIEWS, RESOLVED_NAME, POSTED_BY, THANKED
    }

    private final AppStateService appStateService;

    private Status status;

    private final String type = "post";

    private String postId;

    private String threadTitle;

    private String threadId;

    private String title;

    private String url;

    private List<Image> images;

    private Map<String, Object> metadata;

    private final AtomicInteger done = new AtomicInteger(0);

    private int total;

    private Set<String> hosts;

    private boolean removed = false;

    private String forum;

    private String securityToken;

    @Setter
    private String postFolderName;

    private Post() {
        this.appStateService = SpringContext.getBean(AppStateService.class);
    }

    public Post(String title, String url, List<Image> images, Map<String, Object> metadata, String postId, String threadId, String threadTitle, String forum, String securityToken) throws PostParseException {
        this();
        this.title = title;
        this.url = url;
        this.images = images;
        this.metadata = metadata;
        this.postId = postId;
        this.forum = forum;
        this.threadId = threadId;
        this.threadTitle = threadTitle;
        this.securityToken = securityToken;
        this.hosts = this.images.stream().map(Image::getHost).map(Host::getHost).collect(Collectors.toSet());
        total = images.size();
        status = Status.STOPPED;

        if (!this.appStateService.newPost(this)) {
            throw new PostParseException("Post already loaded");
        }
    }

    public void setTitle(String title) {
        this.title = title;
        updateNotification();
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
            appStateService.postUpdated(this);
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
