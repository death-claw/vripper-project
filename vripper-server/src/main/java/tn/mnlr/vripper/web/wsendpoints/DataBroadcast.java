package tn.mnlr.vripper.web.wsendpoints;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.services.*;
import tn.mnlr.vripper.services.domain.DownloadSpeed;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataBroadcast {

    private final SimpMessagingTemplate template;
    private final VGAuthService VGAuthService;
    private final GlobalStateService globalStateService;
    private final DownloadSpeedService downloadSpeedService;
    private final DataService dataService;

    private final List<Disposable> disposables = new ArrayList<>();

    @Autowired
    public DataBroadcast(SimpMessagingTemplate template, VGAuthService VGAuthService, GlobalStateService globalStateService, DownloadSpeedService downloadSpeedService, DataService dataService) {
        this.template = template;
        this.VGAuthService = VGAuthService;
        this.globalStateService = globalStateService;
        this.downloadSpeedService = downloadSpeedService;
        this.dataService = dataService;
    }

    @PostConstruct
    private void run() {
        disposables.add(VGAuthService.getLoggedInUser()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .map(DataController.LoggedUser::new)
                .subscribe(user -> template.convertAndSend("/topic/user", user), e -> log.error("Failed to send data to client", e)));

        disposables.add(globalStateService.getLiveGlobalState()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .subscribe(state -> template.convertAndSend("/topic/download-state", state), e -> log.error("Failed to send data to client", e)));

        disposables.add(downloadSpeedService.getReadBytesPerSecond()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .map(DownloadSpeed::new)
                .subscribe(speed -> template.convertAndSend("/topic/speed", speed), e -> log.error("Failed to send data to client", e)));

        disposables.add(dataService.livePost()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/posts", ids.stream().map(dataService::findPostById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> log.error("Failed to send data to client", e)));

        disposables.add(dataService.liveImage()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
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

        disposables.add(dataService.liveQueue()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/queued", ids.stream().map(dataService::findQueuedById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> log.error("Failed to send data to client", e))
        );

        disposables.add(dataService.queueRemove()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(threadIds -> template.convertAndSend("/topic/queued/deleted", threadIds), e -> log.error("Failed to send data to client", e))
        );

        disposables.add(dataService.postRemove()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
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
