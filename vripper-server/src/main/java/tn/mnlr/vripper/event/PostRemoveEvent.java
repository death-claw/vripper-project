package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PostRemoveEvent extends ApplicationEvent {

    private final String postId;

    public PostRemoveEvent(Object source, String postId) {
        super(source);
        this.postId = postId;
    }
}
