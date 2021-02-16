package tn.mnlr.vripper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.jpa.repositories.IRepository;
import tn.mnlr.vripper.services.DataService;

import java.util.Set;

@Component
public class EventListenerBean {

    @Getter
    private static boolean init = false;

    private final DataService dataService;
    private final Set<IRepository> repositorySet;

    @Autowired
    public EventListenerBean(DataService dataService, Set<IRepository> repositorySet) {
        this.dataService = dataService;
        this.repositorySet = repositorySet;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        dataService.setDownloadingToStopped();
        init = true;
    }
}
