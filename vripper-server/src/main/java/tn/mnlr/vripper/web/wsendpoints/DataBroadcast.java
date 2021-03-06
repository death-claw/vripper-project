package tn.mnlr.vripper.web.wsendpoints;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import tn.mnlr.vripper.event.Event;
import tn.mnlr.vripper.event.EventBus;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.domain.DownloadSpeed;
import tn.mnlr.vripper.services.domain.GlobalState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataBroadcast {

  private final SimpMessagingTemplate template;
  private final DataService dataService;
  private final EventBus eventBus;

  private Disposable disposable;

  @Autowired
  public DataBroadcast(SimpMessagingTemplate template, DataService dataService, EventBus eventBus) {
    this.template = template;
    this.dataService = dataService;
    this.eventBus = eventBus;
  }

  @PostConstruct
  private void run() {

    disposable =
        eventBus
            .flux()
            .buffer(Duration.of(500, ChronoUnit.MILLIS))
            .subscribe(
                data -> {
                  Map<Event.Kind, List<Event<?>>> eventMap =
                      data.stream().collect(Collectors.groupingBy(Event::getKind));

                  eventMap.forEach(
                      (kind, eventList) -> {
                        switch (kind) {
                          case POST_UPDATE:
                          case METADATA_UPDATE:
                            template.convertAndSend(
                                "/topic/posts",
                                eventList.stream()
                                    .map(e -> ((Long) e.getData()))
                                    .distinct()
                                    .map(dataService::findById)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toUnmodifiableSet()));

                            break;
                          case POST_REMOVE:
                            template.convertAndSend(
                                "/topic/posts/deleted",
                                eventList.stream()
                                    .map(e -> ((String) e.getData()))
                                    .collect(Collectors.toUnmodifiableSet()));
                            break;
                          case IMAGE_UPDATE:
                            eventList.stream()
                                .map(e -> ((Long) e.getData()))
                                .distinct()
                                .map(dataService::findImageById)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.groupingBy(Image::getPostId))
                                .forEach(
                                    (postId, images) ->
                                        template.convertAndSend("/topic/images/" + postId, images));
                            break;

                          case QUEUED_UPDATE:
                            template.convertAndSend(
                                "/topic/queued",
                                eventList.stream()
                                    .map(e -> ((Long) e.getData()))
                                    .distinct()
                                    .map(dataService::findQueuedById)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toUnmodifiableSet()));
                            break;
                          case QUEUED_REMOVE:
                            template.convertAndSend(
                                "/topic/queued/deleted",
                                eventList.stream()
                                    .map(e -> ((String) e.getData()))
                                    .collect(Collectors.toUnmodifiableSet()));
                            break;
                          case LOG_EVENT_UPDATE:
                            template.convertAndSend(
                                "/topic/events",
                                eventList.stream()
                                    .map(e -> ((Long) e.getData()))
                                    .distinct()
                                    .map(dataService::findEventById)
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toUnmodifiableSet()));
                            break;
                          case LOG_EVENT_REMOVE:
                            template.convertAndSend(
                                "/topic/events/deleted",
                                eventList.stream()
                                    .map(e -> ((Long) e.getData()))
                                    .collect(Collectors.toUnmodifiableSet()));
                            break;
                          case VG_USER:
                            eventList.stream()
                                .map(e -> new DataController.LoggedUser((String) e.getData()))
                                .distinct()
                                .forEach(user -> template.convertAndSend("/topic/user", user));
                            break;
                          case GLOBAL_STATE:
                            eventList.stream()
                                .map(e -> ((GlobalState) e.getData()))
                                .distinct()
                                .forEach(
                                    globalState ->
                                        template.convertAndSend(
                                            "/topic/download-state", globalState));
                            break;
                          case BYTES_PER_SECOND:
                            eventList.stream()
                                .map(e -> new DownloadSpeed((Long) e.getData()))
                                .distinct()
                                .forEach(speed -> template.convertAndSend("/topic/speed", speed));

                            break;
                        }
                      });
                });
  }

  @PreDestroy
  private void destroy() {
    if (disposable != null) {
      disposable.dispose();
    }
  }
}
