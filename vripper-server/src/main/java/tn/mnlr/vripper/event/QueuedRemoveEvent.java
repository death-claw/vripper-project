package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class QueuedRemoveEvent extends ApplicationEvent {

  private final String threadId;

  public QueuedRemoveEvent(Object source, String threadId) {
    super(source);
    this.threadId = threadId;
  }
}
