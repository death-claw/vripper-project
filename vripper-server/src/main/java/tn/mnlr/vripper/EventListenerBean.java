package tn.mnlr.vripper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.services.DataService;

@Component
public class EventListenerBean {

  @Getter private static boolean init = false;

  private final DataService dataService;

  @Autowired
  public EventListenerBean(DataService dataService) {
    this.dataService = dataService;
  }

  @EventListener
  public void onApplicationEvent(ContextRefreshedEvent event) {
    dataService.setDownloadingToStopped();
    init = true;
  }
}
