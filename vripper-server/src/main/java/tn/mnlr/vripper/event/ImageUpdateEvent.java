package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ImageUpdateEvent extends ApplicationEvent {

    private final Long id;

    public ImageUpdateEvent(Object source, Long id) {
        super(source);
        this.id = id;
    }
}
