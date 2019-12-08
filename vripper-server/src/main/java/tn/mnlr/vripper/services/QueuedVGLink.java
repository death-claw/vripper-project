package tn.mnlr.vripper.services;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
public class QueuedVGLink {

    private final String type = "grabQueue";
    private final String link;
    private final String threadId;
    private final String postId;
    @Setter
    boolean removed;

    public QueuedVGLink(String link, String threadId, String postId) {
        this.link = link;
        this.threadId = threadId;
        this.postId = postId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueuedVGLink that = (QueuedVGLink) o;
        return Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }
}
