package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class QueuedUpdateEvent extends ApplicationEvent {

  private final Long id;

  public QueuedUpdateEvent(Object source, Long id) {
    super(source);
    this.id = id;
  }
}
