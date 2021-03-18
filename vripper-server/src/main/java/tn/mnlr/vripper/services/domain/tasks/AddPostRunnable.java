package tn.mnlr.vripper.services.domain.tasks;

import lombok.extern.slf4j.Slf4j;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.download.PendingQueue;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.MetadataService;
import tn.mnlr.vripper.services.SettingsService;
import tn.mnlr.vripper.services.VGAuthService;
import tn.mnlr.vripper.services.domain.PostScanParser;
import tn.mnlr.vripper.services.domain.PostScanResult;

import java.time.LocalDateTime;
import java.util.Set;

import static tn.mnlr.vripper.jpa.domain.Event.Status.ERROR;
import static tn.mnlr.vripper.jpa.domain.Event.Status.PROCESSING;

@Slf4j
public class AddPostRunnable implements Runnable {

    private final String postId;
    private final String threadId;
    private final DataService dataService;
    private final MetadataService metadataService;
    private final SettingsService settingsService;
    private final PendingQueue pendingQueue;
    private final VGAuthService VGAuthService;
    private final Event event;
    private final IEventRepository eventRepository;
    private final String link;

    public AddPostRunnable(String postId, String threadId) {
        this.postId = postId;
        this.threadId = threadId;
        this.dataService = SpringContext.getBean(DataService.class);
        this.metadataService = SpringContext.getBean(MetadataService.class);
        this.settingsService = SpringContext.getBean(SettingsService.class);
        this.pendingQueue = SpringContext.getBean(PendingQueue.class);
        this.VGAuthService = SpringContext.getBean(VGAuthService.class);
        this.eventRepository = SpringContext.getBean(IEventRepository.class);
        link = settingsService.getSettings().getVProxy() + String.format("/threads/%s?%s", threadId, (postId != null ? "p=" + postId : ""));
        event = new Event(Event.Type.POST, Event.Status.PENDING, LocalDateTime.now(), String.format("Processing %s", link));
        eventRepository.save(event);
    }

    @Override
    public void run() {

        try {
            event.setStatus(PROCESSING);
            eventRepository.update(event);
            if (dataService.exists(postId)) {
                log.warn(String.format("skipping %s, already loaded", postId));
                event.setMessage(String.format("Gallery %s is already loaded", link));
                event.setStatus(ERROR);
                eventRepository.update(event);
                return;
            }

            PostScanParser postScanParser = new PostScanParser(threadId, postId);

            PostScanResult postScanResult;
            try {
                postScanResult = postScanParser.parse();
            } catch (PostParseException e) {
                String error = String.format("parsing failed for gallery %s", link);
                log.error(error, e);
                event.setMessage(error + "\n" + Utils.throwableToString(e));
                event.setStatus(ERROR);
                eventRepository.update(event);
                return;
            }
            if (postScanResult.getPost().isEmpty()) {
                String error = String.format("Gallery %s contains no galleries", link);
                log.error(error);
                event.setMessage(error);
                event.setStatus(ERROR);
                eventRepository.update(event);
                return;
            }
            if (postScanResult.getImages().isEmpty()) {
                String error = String.format("Gallery %s contains no images to download", link);
                log.error(error);
                event.setMessage(error);
                event.setStatus(ERROR);
                eventRepository.update(event);
                return;
            }

            Post post = postScanResult.getPost().get();
            Set<Image> images = postScanResult.getImages();

            dataService.newPost(post, images);

            metadataService.startFetchingMetadata(post);

            if (settingsService.getSettings().getAutoStart()) {
                log.debug("Auto start downloads option is enabled");
                post.setStatus(Status.PENDING);
                try {
                    pendingQueue.enqueue(post, images);
                } catch (InterruptedException e) {
                    log.warn("Interruption was caught");
                    Thread.currentThread().interrupt();
                    return;
                }
                log.debug(String.format("Done enqueuing jobs for %s", post.getUrl()));
            } else {
                post.setStatus(Status.STOPPED);
                log.debug("Auto start downloads option is disabled");
            }
            if (!settingsService.getSettings().getLeaveThanksOnStart()) {
                VGAuthService.leaveThanks(post);
            }
            dataService.updatePostStatus(post.getStatus(), post.getId());
            event.setMessage(String.format("Gallery %s is successfully added to download queue", link));
            event.setStatus(Event.Status.DONE);
            eventRepository.update(event);
        } catch (Exception e) {
            String error = String.format("Error when adding gallery %s", link);
            log.error(error, e);
            event.setMessage(error + "\n" + Utils.throwableToString(e));
            event.setStatus(ERROR);
            eventRepository.update(event);
        }
    }
}
