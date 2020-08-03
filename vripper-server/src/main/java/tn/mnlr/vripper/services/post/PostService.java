package tn.mnlr.vripper.services.post;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.q.PendingQ;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.CommonExecutor;
import tn.mnlr.vripper.services.DataService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Service
@Slf4j
public class PostService {

    private final AppSettingsService appSettingsService;
    private final PendingQ pendingQ;
    private final DataService dataService;
    private final CommonExecutor commonExecutor;
    private final Map<String, Future<?>> fetchingMetadata = new ConcurrentHashMap<>();

    @Autowired
    public PostService(AppSettingsService appSettingsService, PendingQ pendingQ, DataService dataService, DataService dataService1, CommonExecutor commonExecutor) {
        this.appSettingsService = appSettingsService;
        this.pendingQ = pendingQ;
        this.dataService = dataService1;
        this.commonExecutor = commonExecutor;
    }

    public void addPost(String postId, String threadId) throws PostParseException {

        if (dataService.exists(postId)) {
            log.warn(String.format("skipping %s, already loaded", postId));
            return;
        }

        VRPostParser vrPostParser = new VRPostParser(threadId, postId);
        ParseResult parseResult = vrPostParser.parse();
        if (parseResult.getPost().isEmpty()) {
            throw new PostParseException(String.format("parsing failed for thread %s, post %s", threadId, postId));
        }

        Post post = parseResult.getPost().get();
        Set<Image> images = parseResult.getImages();

        dataService.newPost(post, images);

        // Metadata thread
        fetchingMetadata.put(post.getPostId(), commonExecutor.getGeneralExecutor().submit(new MetadataRunnable(post)));

        if (appSettingsService.getSettings().getAutoStart()) {
            log.debug("Auto start downloads option is enabled");
            post.setStatus(Status.PENDING);
            try {
                pendingQ.enqueue(post, images);
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
        dataService.updatePostStatus(post.getStatus(), post.getId());
    }

    public void stopFetchingMetadata(Post post) {
        this.fetchingMetadata.forEach((k, v) -> {
            if (k.equals(post.getPostId())) {
                v.cancel(true);
            }
        });
        fetchingMetadata.remove(post.getPostId());
    }
}
