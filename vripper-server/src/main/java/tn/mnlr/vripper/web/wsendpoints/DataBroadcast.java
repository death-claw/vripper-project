package tn.mnlr.vripper.web.wsendpoints;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import tn.mnlr.vripper.event.ImageUpdateEvent;
import tn.mnlr.vripper.event.MetadataUpdateEvent;
import tn.mnlr.vripper.event.PostUpdateEvent;
import tn.mnlr.vripper.event.QueuedUpdateEvent;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.listener.*;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.DownloadSpeedService;
import tn.mnlr.vripper.services.GlobalStateService;
import tn.mnlr.vripper.services.VGAuthService;
import tn.mnlr.vripper.services.domain.DownloadSpeed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataBroadcast {

    private final SimpMessagingTemplate template;
    private final VGAuthService VGAuthService;
    private final GlobalStateService globalStateService;
    private final DownloadSpeedService downloadSpeedService;
    private final DataService dataService;

    private final PostUpdateEventListener postUpdateEventListener;
    private final MetadataUpdateEventListener metadataUpdateEventListener;
    private final ImageUpdateEventListener imageUpdateEventListener;
    private final QueuedUpdateEventListener queuedUpdateEventListener;

    private final QueuedRemoveEventListener queuedRemoveEventListener;
    private final PostRemoveEventListener postRemoveEventListener;

    private final List<Disposable> disposables = new ArrayList<>();

    @Autowired
    public DataBroadcast(SimpMessagingTemplate template, VGAuthService VGAuthService, GlobalStateService globalStateService, DownloadSpeedService downloadSpeedService, DataService dataService, PostUpdateEventListener postUpdateEventListener, MetadataUpdateEventListener metadataUpdateEventListener, ImageUpdateEventListener imageUpdateEventListener, QueuedUpdateEventListener queuedUpdateEventListener, QueuedRemoveEventListener queuedRemoveEventListener, PostRemoveEventListener postRemoveEventListener) {
        this.template = template;
        this.VGAuthService = VGAuthService;
        this.globalStateService = globalStateService;
        this.downloadSpeedService = downloadSpeedService;
        this.dataService = dataService;
        this.postUpdateEventListener = postUpdateEventListener;
        this.metadataUpdateEventListener = metadataUpdateEventListener;
        this.imageUpdateEventListener = imageUpdateEventListener;
        this.queuedUpdateEventListener = queuedUpdateEventListener;
        this.queuedRemoveEventListener = queuedRemoveEventListener;
        this.postRemoveEventListener = postRemoveEventListener;
    }

    @PostConstruct
    private void run() {
        disposables.add(VGAuthService.getLoggedInUser()
                .map(DataController.LoggedUser::new)
                .subscribe(user -> template.convertAndSend("/topic/user", user), e -> log.error("Failed to send data to client", e)));

        disposables.add(globalStateService.getGlobalState()
                .subscribe(state -> template.convertAndSend("/topic/download-state", state), e -> log.error("Failed to send data to client", e)));

        disposables.add(downloadSpeedService.getReadBytesPerSecond()
                .map(DownloadSpeed::new)
                .subscribe(speed -> template.convertAndSend("/topic/speed", speed), e -> log.error("Failed to send data to client", e)));

        disposables.add(postUpdateEventListener.getDataFlux()
                .map(PostUpdateEvent::getId)
                .buffer(Duration.of(500, ChronoUnit.MILLIS))
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/posts", ids.stream().map(dataService::findById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> log.error("Failed to send data to client", e)));

        disposables.add(metadataUpdateEventListener.getDataFlux()
                .map(MetadataUpdateEvent::getPostIdRef)
                .buffer(Duration.of(500, ChronoUnit.MILLIS))
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/posts", ids.stream().map(dataService::findById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> log.error("Failed to send data to client", e)));

        disposables.add(imageUpdateEventListener.getDataFlux()
                .map(ImageUpdateEvent::getId)
                .buffer(Duration.of(500, ChronoUnit.MILLIS))
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(id -> id
                                .stream()
                                .map(dataService::findImageById)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.groupingBy(Image::getPostId))
                                .forEach((postId, images) -> template.convertAndSend("/topic/images/" + postId, images)),
                        e -> log.error("Failed to send data to client", e))
        );

        disposables.add(queuedUpdateEventListener.getDataFlux()
                .map(QueuedUpdateEvent::getId)
                .buffer(Duration.of(500, ChronoUnit.MILLIS))
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/queued", ids.stream().map(dataService::findQueuedById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> log.error("Failed to send data to client", e))
        );

        disposables.add(queuedRemoveEventListener.getDataFlux()
                .buffer(Duration.of(500, ChronoUnit.MILLIS))
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(threadIds -> template.convertAndSend("/topic/queued/deleted", threadIds), e -> log.error("Failed to send data to client", e))
        );

        disposables.add(postRemoveEventListener.getDataFlux()
                .buffer(Duration.of(500, ChronoUnit.MILLIS))
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(postIds -> template.convertAndSend("/topic/posts/deleted", postIds), e -> log.error("Failed to send data to client", e))
        );
    }

    @PreDestroy
    private void destroy() {
        disposables.forEach(Disposable::dispose);
    }
}
