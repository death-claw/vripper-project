package tn.mnlr.vripper.web.wsendpoints;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.LogEvent;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.DownloadSpeedService;
import tn.mnlr.vripper.services.GlobalStateService;
import tn.mnlr.vripper.services.VGAuthService;
import tn.mnlr.vripper.services.domain.DownloadSpeed;
import tn.mnlr.vripper.services.domain.GlobalState;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Controller
public class DataController {

  private final VGAuthService VGAuthService;
  private final GlobalStateService globalStateService;
  private final DownloadSpeedService downloadSpeedService;
  private final DataService dataService;

  @Autowired
  public DataController(
      VGAuthService VGAuthService,
      GlobalStateService globalStateService,
      DownloadSpeedService downloadSpeedService,
      DataService dataService) {
    this.VGAuthService = VGAuthService;
    this.globalStateService = globalStateService;
    this.downloadSpeedService = downloadSpeedService;
    this.dataService = dataService;
  }

  @SubscribeMapping("/user")
  public LoggedUser user() {
    return new LoggedUser(VGAuthService.getLoggedUser());
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
    List<Post> posts = dataService.findAllPosts();
    posts.sort(Comparator.comparing(Post::getAddedOn));
    return posts;
  }

  @SubscribeMapping("/images/{postId}")
  public List<Image> postsDetails(@DestinationVariable("postId") String postId) {
    return dataService.findImagesByPostId(postId);
  }

  @SubscribeMapping("/queued")
  public Collection<Queued> queued() {
    return dataService.findAllQueued();
  }

  @SubscribeMapping("/events")
  public Collection<LogEvent> events() {
    return dataService.findAllEvents();
  }

  @Getter
  public static class LoggedUser {

    private final String user;

    LoggedUser(String user) {
      this.user = user;
    }
  }
}
