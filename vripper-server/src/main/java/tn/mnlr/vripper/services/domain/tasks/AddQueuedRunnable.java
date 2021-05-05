package tn.mnlr.vripper.services.domain.tasks;

import lombok.extern.slf4j.Slf4j;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.PostService;
import tn.mnlr.vripper.services.ThreadPoolService;
import tn.mnlr.vripper.services.domain.MultiPostScanResult;

import java.time.LocalDateTime;

import static tn.mnlr.vripper.jpa.domain.Event.Status.ERROR;
import static tn.mnlr.vripper.jpa.domain.Event.Status.PROCESSING;

@Slf4j
public class AddQueuedRunnable implements Runnable {

  private final Queued queued;
  private final ThreadPoolService threadPoolService;
  private final DataService dataService;
  private final IEventRepository eventRepository;
  private final PostService postService;
  private final Event event;

  public AddQueuedRunnable(Queued queued) {
    this.queued = queued;
    threadPoolService = SpringContext.getBean(ThreadPoolService.class);
    dataService = SpringContext.getBean(DataService.class);
    eventRepository = SpringContext.getBean(IEventRepository.class);
    postService = SpringContext.getBean(PostService.class);
    event =
        new Event(
            Event.Type.QUEUED,
            Event.Status.PENDING,
            LocalDateTime.now(),
            String.format("Processing multi-post link %s", queued.getLink()));
    eventRepository.save(event);
  }

  @Override
  public void run() {
    try {
      event.setStatus(PROCESSING);
      eventRepository.update(event);
      MultiPostScanResult multiPostScanResult = postService.get(queued);
      if (multiPostScanResult == null) {
        String message = String.format("Fetching multi-post link %s failed", queued.getLink());
        event.setStatus(ERROR);
        event.setMessage(message);
        eventRepository.update(event);
        return;
      } else if (multiPostScanResult.getError() != null) {
        String message =
            "Nothing found for " + queued.getLink() + "\n" + multiPostScanResult.getError();
        event.setStatus(ERROR);
        event.setMessage(message);
        eventRepository.update(event);
        return;
      } else if (multiPostScanResult.getPosts().isEmpty()) {
        String message = "Nothing found for " + queued.getLink();
        event.setStatus(ERROR);
        event.setMessage(message);
        eventRepository.update(event);
        return;
      }
      queued.setTotal(multiPostScanResult.getPosts().size());
      queued.done();
      if (multiPostScanResult.getPosts().size() == 1) {
        threadPoolService
            .getGeneralExecutor()
            .submit(
                new AddPostRunnable(
                    multiPostScanResult.getPosts().get(0).getPostId(),
                    multiPostScanResult.getPosts().get(0).getThreadId()));
        event.setStatus(Event.Status.DONE);
        event.setMessage(String.format("Link %s is added to download queue", queued.getLink()));
      } else {
        if (dataService.findQueuedByThreadId(queued.getThreadId()).isEmpty()) {
          dataService.newQueueLink(queued);
          event.setStatus(Event.Status.DONE);
          event.setMessage(String.format("Link %s is added to multi-post links", queued.getLink()));
        } else {
          log.info(String.format("Link %s is already loaded", queued.getLink()));
          event.setStatus(Event.Status.ERROR);
          event.setMessage(
              String.format("%s has already been added to multi-post links", queued.getLink()));
        }
      }
      eventRepository.update(event);
    } catch (Exception e) {
      String error = String.format("Error when adding multi-post link %s", queued.getLink());
      log.error(error, e);
      event.setMessage(error + "\n" + Utils.throwableToString(e));
      event.setStatus(ERROR);
      eventRepository.update(event);
    }
  }
}
