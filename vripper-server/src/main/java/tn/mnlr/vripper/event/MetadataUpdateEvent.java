package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MetadataUpdateEvent extends ApplicationEvent {

    private final Long postIdRef;

    public MetadataUpdateEvent(Object source, Long postIdRef) {
        super(source);
        this.postIdRef = postIdRef;
    }
}
