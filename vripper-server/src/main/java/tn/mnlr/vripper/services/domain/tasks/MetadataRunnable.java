package tn.mnlr.vripper.services.domain.tasks;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.MetadataService;

import java.time.LocalDateTime;

import static tn.mnlr.vripper.jpa.domain.Event.Status.ERROR;

@Slf4j
public class MetadataRunnable implements Runnable {

  @Getter private final Post post;

  private final MetadataService metadataService;
  private final DataService dataService;
  private final IEventRepository eventRepository;

  private final Event event;

  @Setter private volatile boolean interrupted = false;

  public MetadataRunnable(@NonNull Post post) {
    this.post = post;
    metadataService = SpringContext.getBean(MetadataService.class);
    dataService = SpringContext.getBean(DataService.class);
    eventRepository = SpringContext.getBean(IEventRepository.class);
    event =
        new Event(
            Event.Type.METADATA,
            Event.Status.PENDING,
            LocalDateTime.now(),
            "Fetching metadata for " + post.getUrl());
    eventRepository.save(event);
  }

  @Override
  public void run() {

    try {
      event.setStatus(Event.Status.PROCESSING);
      eventRepository.update(event);
      if (interrupted) {
        String message = String.format("Fetching metadata for %s interrupted", post.getUrl());
        event.setStatus(Event.Status.DONE);
        event.setMessage(message);
        eventRepository.update(event);
        return;
      }

      Metadata metadata = metadataService.get(post);
      if (metadata == null) {
        String message = String.format("Fetching metadata for %s failed", post.getUrl());
        event.setStatus(ERROR);
        event.setMessage(message);
        eventRepository.update(event);
        return;
      }
      dataService.setMetadata(post, metadata);

      event.setStatus(Event.Status.DONE);
      eventRepository.update(event);
    } catch (Exception e) {
      String message = String.format("Failed to fetch metadata for %s", post.getUrl());
      log.error(message, e);
      event.setMessage(message + "\n" + Utils.throwableToString(e));
      event.setStatus(ERROR);
      eventRepository.update(event);
    }
  }
}
