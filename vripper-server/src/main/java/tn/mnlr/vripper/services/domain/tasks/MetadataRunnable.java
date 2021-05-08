package tn.mnlr.vripper.services.domain.tasks;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.LogEvent;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.repositories.ILogEventRepository;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.MetadataService;

import java.time.LocalDateTime;

import static tn.mnlr.vripper.jpa.domain.LogEvent.Status.ERROR;

@Slf4j
public class MetadataRunnable implements Runnable {

  @Getter private final Post post;

  private final MetadataService metadataService;
  private final DataService dataService;
  private final ILogEventRepository eventRepository;

  private final LogEvent logEvent;

  @Setter private volatile boolean interrupted = false;

  public MetadataRunnable(@NonNull Post post) {
    this.post = post;
    metadataService = SpringContext.getBean(MetadataService.class);
    dataService = SpringContext.getBean(DataService.class);
    eventRepository = SpringContext.getBean(ILogEventRepository.class);
    logEvent =
        new LogEvent(
            LogEvent.Type.METADATA,
            LogEvent.Status.PENDING,
            LocalDateTime.now(),
            "Fetching metadata for " + post.getUrl());
    eventRepository.save(logEvent);
  }

  @Override
  public void run() {

    try {
      logEvent.setStatus(LogEvent.Status.PROCESSING);
      eventRepository.update(logEvent);
      if (interrupted) {
        String message = String.format("Fetching metadata for %s interrupted", post.getUrl());
        logEvent.setStatus(LogEvent.Status.DONE);
        logEvent.setMessage(message);
        eventRepository.update(logEvent);
        return;
      }

      Metadata metadata = metadataService.get(post);
      if (metadata == null) {
        String message = String.format("Fetching metadata for %s failed", post.getUrl());
        logEvent.setStatus(ERROR);
        logEvent.setMessage(message);
        eventRepository.update(logEvent);
        return;
      }
      dataService.setMetadata(post, metadata);

      logEvent.setStatus(LogEvent.Status.DONE);
      eventRepository.update(logEvent);
    } catch (Exception e) {
      String message = String.format("Failed to fetch metadata for %s", post.getUrl());
      log.error(message, e);
      logEvent.setMessage(message + "\n" + Utils.throwableToString(e));
      logEvent.setStatus(ERROR);
      eventRepository.update(logEvent);
    }
  }
}
