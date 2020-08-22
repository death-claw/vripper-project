package tn.mnlr.vripper.web.wsendpoints;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.services.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebSocketBroadcast {

    private final SimpMessagingTemplate template;
    private final VipergirlsAuthService vipergirlsAuthService;
    private final GlobalStateService globalStateService;
    private final DownloadSpeedService downloadSpeedService;
    private final DataService dataService;
    private final PathService pathService;

    private final List<Disposable> disposables = new ArrayList<>();

    @Autowired
    public WebSocketBroadcast(SimpMessagingTemplate template, VipergirlsAuthService vipergirlsAuthService, GlobalStateService globalStateService, DownloadSpeedService downloadSpeedService, DataService dataService, PathService pathService) {
        this.template = template;
        this.vipergirlsAuthService = vipergirlsAuthService;
        this.globalStateService = globalStateService;
        this.downloadSpeedService = downloadSpeedService;
        this.dataService = dataService;
        this.pathService = pathService;
    }

    @PostConstruct
    private void run() {
        disposables.add(vipergirlsAuthService.getLoggedInUser()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .map(AppDataController.LoggedUser::new)
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
