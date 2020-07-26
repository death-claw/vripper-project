package tn.mnlr.vripper.web.wsendpoints;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class WebSocketBroadcast {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketBroadcast.class);

    private final SimpMessagingTemplate template;
    private final VipergirlsAuthService vipergirlsAuthService;
    private final GlobalStateService globalStateService;
    private final DownloadSpeedService downloadSpeedService;
    private final PostDataService postDataService;

    private final List<Disposable> disposables = new ArrayList<>();

    @Autowired
    public WebSocketBroadcast(SimpMessagingTemplate template, VipergirlsAuthService vipergirlsAuthService, GlobalStateService globalStateService, DownloadSpeedService downloadSpeedService, PostDataService postDataService) {
        this.template = template;
        this.vipergirlsAuthService = vipergirlsAuthService;
        this.globalStateService = globalStateService;
        this.downloadSpeedService = downloadSpeedService;
        this.postDataService = postDataService;
    }

    @PostConstruct
    private void run() {
        disposables.add(vipergirlsAuthService.getLoggedInUser()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .map(AppDataController.LoggedUser::new)
                .subscribe(user -> template.convertAndSend("/topic/user", user), e -> logger.error("Failed to send data to client", e)));

        disposables.add(globalStateService.getLiveGlobalState()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .subscribe(state -> template.convertAndSend("/topic/download-state", state), e -> logger.error("Failed to send data to client", e)));

        disposables.add(downloadSpeedService.getReadBytesPerSecond()
                .subscribeOn(Schedulers.io())
                .onBackpressureBuffer()
                .map(DownloadSpeed::new)
                .subscribe(speed -> template.convertAndSend("/topic/speed", speed), e -> logger.error("Failed to send data to client", e)));

        disposables.add(postDataService.livePost()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/posts", ids.stream().map(postDataService::findPostById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> logger.error("Failed to send data to client", e)));

        disposables.add(postDataService.liveImage()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(id -> id
                                .stream()
                                .map(postDataService::findImageById)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.groupingBy(Image::getPostId))
                                .forEach((postId, images) -> template.convertAndSend("/topic/images/" + postId, images)),
                        e -> logger.error("Failed to send data to client", e))
        );

        disposables.add(postDataService.liveQueue()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(ids -> template.convertAndSend("/topic/queued", ids.stream().map(postDataService::findQueuedById).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList())), e -> logger.error("Failed to send data to client", e))
        );

        disposables.add(postDataService.queueRemove()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(threadIds -> template.convertAndSend("/topic/queued/deleted", threadIds), e -> logger.error("Failed to send data to client", e))
        );

        disposables.add(postDataService.postRemove()
                .subscribeOn(Schedulers.io())
                .buffer(500, TimeUnit.MILLISECONDS)
                .map(HashSet::new)
                .filter(e -> !e.isEmpty())
                .subscribe(postIds -> template.convertAndSend("/topic/posts/deleted", postIds), e -> logger.error("Failed to send data to client", e))
        );
    }

    @PreDestroy
    private void destroy() {
        disposables.forEach(Disposable::dispose);
    }
}