package tn.mnlr.vripper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tn.mnlr.vripper.jpa.repositories.IRepository;
import tn.mnlr.vripper.services.PostDataService;

import java.util.Set;

@Component
public class EventListenerBean {

    @Getter
    private static boolean init = false;

    private final PostDataService postDataService;
    private final Set<IRepository> repositorySet;

    @Autowired
    public EventListenerBean(PostDataService postDataService, Set<IRepository> repositorySet) {
        this.postDataService = postDataService;
        this.repositorySet = repositorySet;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        repositorySet.forEach(IRepository::init);
        postDataService.setDownloadingToStopped();
        init = true;
    }


}