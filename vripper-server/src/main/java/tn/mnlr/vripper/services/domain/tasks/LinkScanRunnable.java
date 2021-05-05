package tn.mnlr.vripper.services.domain.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.jpa.repositories.impl.EventRepository;
import tn.mnlr.vripper.services.PostService;
import tn.mnlr.vripper.services.SettingsService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tn.mnlr.vripper.jpa.domain.Event.Status.ERROR;

@Slf4j
public class LinkScanRunnable implements Runnable {

  private static final Object LOCK = new Object();
  private final List<String> urlList;
  private final SettingsService settingsService;
  private final PostService postService;
  private final EventRepository eventRepository;
  private final Event event;

  public LinkScanRunnable(@NonNull List<String> urlList) {
    this.urlList = urlList;
    settingsService = SpringContext.getBean(SettingsService.class);
    postService = SpringContext.getBean(PostService.class);
    eventRepository = SpringContext.getBean(EventRepository.class);
    event =
        new Event(
            Event.Type.SCAN,
            Event.Status.PENDING,
            LocalDateTime.now(),
            "Links to scan:\n\t" + String.join("\n\t", urlList));
    eventRepository.save(event);
  }

  @Override
  public void run() {
    synchronized (LOCK) {
      try {
        event.setStatus(Event.Status.PROCESSING);
        eventRepository.update(event);
        ArrayList<Queued> queuedList = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        List<String> unrecognized = new ArrayList<>();
        for (String url : urlList) {
          log.debug(String.format("Starting to process thread: %s", url));
          if (!url.startsWith(settingsService.getSettings().getVProxy())) {
            log.error(String.format("Unsupported link %s", url));
            unsupported.add(url);
            continue;
          }

          String threadId, postId;
          Matcher m =
              Pattern.compile(
                      Pattern.quote(settingsService.getSettings().getVProxy())
                          + "/threads/(\\d+)((.*p=)(\\d+))?")
                  .matcher(url);
          if (m.find()) {
            threadId = m.group(1);
            postId = m.group(4);
          } else {
            log.error(String.format("Cannot retrieve thread id from URL %s", url));
            unrecognized.add(url);
            continue;
          }
          queuedList.add(new Queued(url, threadId, postId));
        }
        StringBuilder errorMessage = new StringBuilder();
        if (!unsupported.isEmpty()) {
          errorMessage
              .append("Unsupported links:\n\t")
              .append(String.join("\n\t", unsupported))
              .append("\n\n");
        }
        if (!unrecognized.isEmpty()) {
          errorMessage
              .append("Unrecognized links:\n\t")
              .append(String.join("\n\t", unrecognized))
              .append("\n\n");
        }

        postService.processMultiPost(queuedList);
        if (!unsupported.isEmpty() || !unrecognized.isEmpty()) {
          event.setStatus(Event.Status.ERROR);
          event.setMessage("Some links failed to be scanned: \n" + errorMessage);
        } else {
          event.setStatus(Event.Status.DONE);
        }
        eventRepository.update(event);
      } catch (Exception e) {
        String error = "Error when scanning links";
        log.error(error, e);
        event.setMessage(error + "\n" + Utils.throwableToString(e));
        event.setStatus(ERROR);
        eventRepository.update(event);
      }
    }
  }
}
