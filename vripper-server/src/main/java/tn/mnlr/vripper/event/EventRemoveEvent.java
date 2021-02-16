package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventRemoveEvent extends ApplicationEvent {

    private final Long id;

    public EventRemoveEvent(Object source, Long id) {
        super(source);
        this.id = id;
    }
}
