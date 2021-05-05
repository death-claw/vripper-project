package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventUpdateEvent extends ApplicationEvent {

  private final Long id;

  public EventUpdateEvent(Object source, Long id) {
    super(source);
    this.id = id;
  }
}
