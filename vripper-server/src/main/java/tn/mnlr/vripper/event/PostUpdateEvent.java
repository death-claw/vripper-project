package tn.mnlr.vripper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PostUpdateEvent extends ApplicationEvent {

  private final Long id;

  public PostUpdateEvent(Object source, Long id) {
    super(source);
    this.id = id;
  }
}
