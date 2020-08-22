package tn.mnlr.vripper.web.wsendpoints;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.services.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
public class AppDataController {

    private final VipergirlsAuthService vipergirlsAuthService;
    private final GlobalStateService globalStateService;
    private final DownloadSpeedService downloadSpeedService;
    private final DataService dataService;
    private final PathService pathService;

    @Autowired
    public AppDataController(VipergirlsAuthService vipergirlsAuthService, GlobalStateService globalStateService, DownloadSpeedService downloadSpeedService, DataService dataService, PathService pathService) {
        this.vipergirlsAuthService = vipergirlsAuthService;
        this.globalStateService = globalStateService;
        this.downloadSpeedService = downloadSpeedService;
        this.dataService = dataService;
        this.pathService = pathService;
    }

    @Getter
    public static class LoggedUser {

        private final String user;

        LoggedUser(String user) {
            this.user = user;
        }
    }

    @SubscribeMapping("/user")
    public LoggedUser user() {
        return new LoggedUser(vipergirlsAuthService.getLoggedUser());
    }

    @SubscribeMapping("/download-state")
    public GlobalState downloadState() {
        return globalStateService.getCurrentState();
    }

    @SubscribeMapping("/speed")
    public DownloadSpeed speed() {
        return new DownloadSpeed(downloadSpeedService.getCurrentValue());
    }

    @SubscribeMapping("/posts")
    public Collection<Post> posts() {
        return StreamSupport.stream(dataService.findAllPosts().spliterator(), false).collect(Collectors.toList());
    }

    @SubscribeMapping("/images/{postId}")
    public List<Image> postsDetails(@DestinationVariable("postId") String postId) {
        return dataService.findImagesByPostId(postId);
    }

    @SubscribeMapping("/queued")
    public Collection<Queued> queued() {
        return StreamSupport.stream(dataService.findAllQueued().spliterator(), false).collect(Collectors.toList());
    }
}
