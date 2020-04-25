package tn.mnlr.vripper.services;

import lombok.Getter;
import lombok.ToString;
import tn.mnlr.vripper.SpringContext;

import java.util.Objects;

@ToString
@Getter
public class QueuedVGLink {

    private final String type = "grabQueue";

    private final AppStateService appStateService;

    private final String link;
    private final String threadId;
    private final String postId;

    private int count = 0;
    private boolean loading = true;
    private boolean removed = false;


    public QueuedVGLink(String link, String threadId, String postId) {
        this.appStateService = SpringContext.getBean(AppStateService.class);
        this.link = link;
        this.threadId = threadId;
        this.postId = postId;
    }

    public void done() {
        this.loading = false;
        this.appStateService.queueLinkUpdated(this);
    }

    public void increment() {
        this.count++;
        this.appStateService.queueLinkUpdated(this);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void remove() {
        this.removed = true;
        appStateService.queueLinkUpdated(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueuedVGLink that = (QueuedVGLink) o;
        return Objects.equals(threadId, that.threadId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadId);
    }
}
