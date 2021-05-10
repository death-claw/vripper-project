package tn.mnlr.vripper.tasks;

import lombok.extern.slf4j.Slf4j;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.download.DownloadService;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.LogEvent;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.ILogEventRepository;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.MetadataService;
import tn.mnlr.vripper.services.SettingsService;
import tn.mnlr.vripper.services.VGAuthService;
import tn.mnlr.vripper.services.domain.PostScanParser;
import tn.mnlr.vripper.services.domain.PostScanResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static tn.mnlr.vripper.jpa.domain.LogEvent.Status.ERROR;
import static tn.mnlr.vripper.jpa.domain.LogEvent.Status.PROCESSING;

@Slf4j
public class AddPostRunnable implements Runnable {

  private final String postId;
  private final String threadId;
  private final DataService dataService;
  private final MetadataService metadataService;
  private final SettingsService settingsService;
  private final VGAuthService VGAuthService;
  private final LogEvent logEvent;
  private final ILogEventRepository eventRepository;
  private final String link;
  private final DownloadService downloadService;

  public AddPostRunnable(String postId, String threadId) {
    this.postId = postId;
    this.threadId = threadId;
    this.dataService = SpringContext.getBean(DataService.class);
    this.metadataService = SpringContext.getBean(MetadataService.class);
    this.settingsService = SpringContext.getBean(SettingsService.class);
    this.downloadService = SpringContext.getBean(DownloadService.class);
    this.VGAuthService = SpringContext.getBean(VGAuthService.class);
    this.eventRepository = SpringContext.getBean(ILogEventRepository.class);
    link =
        settingsService.getSettings().getVProxy()
            + String.format("/threads/%s?%s", threadId, (postId != null ? "p=" + postId : ""));
    logEvent =
        new LogEvent(
            LogEvent.Type.POST,
            LogEvent.Status.PENDING,
            LocalDateTime.now(),
            String.format("Processing %s", link));
    eventRepository.save(logEvent);
  }

  @Override
  public void run() {

    try {
      logEvent.setStatus(PROCESSING);
      eventRepository.update(logEvent);
      if (dataService.exists(postId)) {
        log.warn(String.format("skipping %s, already loaded", postId));
        logEvent.setMessage(String.format("Gallery %s is already loaded", link));
        logEvent.setStatus(ERROR);
        eventRepository.update(logEvent);
        return;
      }

      PostScanParser postScanParser = new PostScanParser(threadId, postId);

      PostScanResult postScanResult;
      try {
        postScanResult = postScanParser.parse();
      } catch (PostParseException e) {
        String error = String.format("parsing failed for gallery %s", link);
        log.error(error, e);
        logEvent.setMessage(error + "\n" + Utils.throwableToString(e));
        logEvent.setStatus(ERROR);
        eventRepository.update(logEvent);
        return;
      }
      if (postScanResult.getPost().isEmpty()) {
        String error = String.format("Gallery %s contains no galleries", link);
        log.error(error);
        logEvent.setMessage(error);
        logEvent.setStatus(ERROR);
        eventRepository.update(logEvent);
        return;
      }
      if (postScanResult.getImages().isEmpty()) {
        String error = String.format("Gallery %s contains no images to download", link);
        log.error(error);
        logEvent.setMessage(error);
        logEvent.setStatus(ERROR);
        eventRepository.update(logEvent);
        return;
      }

      Post post = postScanResult.getPost().get();
      Set<Image> images = postScanResult.getImages();

      dataService.newPost(post, images);

      metadataService.startFetchingMetadata(post);

      if (settingsService.getSettings().getAutoStart()) {
        log.debug("Auto start downloads option is enabled");
        post.setStatus(Status.PENDING);
        downloadService.enqueue(Map.of(post, images));
        log.debug(String.format("Done enqueuing jobs for %s", post.getUrl()));
      } else {
        post.setStatus(Status.STOPPED);
        log.debug("Auto start downloads option is disabled");
      }
      if (settingsService.getSettings().getLeaveThanksOnStart() != null
          && !settingsService.getSettings().getLeaveThanksOnStart()) {
        VGAuthService.leaveThanks(post);
      }
      dataService.updatePostStatus(post.getStatus(), post.getId());
      logEvent.setMessage(
          String.format("Gallery %s is successfully added to download queue", link));
      logEvent.setStatus(LogEvent.Status.DONE);
      eventRepository.update(logEvent);
    } catch (Exception e) {
      String error = String.format("Error when adding gallery %s", link);
      log.error(error, e);
      logEvent.setMessage(error + "\n" + Utils.throwableToString(e));
      logEvent.setStatus(ERROR);
      eventRepository.update(logEvent);
    }
  }
}
