package tn.mnlr.vripper.web.restendpoints.domain.posts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ThreadId {
    public ThreadId(String threadId) {
        this.threadId = threadId;
    }

    private String threadId;
}