package tn.mnlr.vripper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.jpa.repositories.IPostRepository;

@Component
public class EventListenerBean {

  @Getter private static boolean init = false;

  private final IPostRepository postRepository;

  @Autowired
  public EventListenerBean(IPostRepository postRepository) {
    this.postRepository = postRepository;
  }

  @EventListener
  public void onApplicationEvent(ContextRefreshedEvent event) {
    postRepository.setDownloadingToStopped();
    init = true;
  }
}
