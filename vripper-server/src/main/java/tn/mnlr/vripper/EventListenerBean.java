package tn.mnlr.vripper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.services.PersistenceService;

@Component
public class EventListenerBean {

    @Autowired
    private PersistenceService persistenceService;

    @Getter
    private static boolean init = false;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        init = true;
        persistenceService.restore();
    }
}