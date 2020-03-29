package tn.mnlr.vripper.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.q.DownloadQ;

@Service
public class PostParser {

    private static final Logger logger = LoggerFactory.getLogger(PostParser.class);

    static final String VR_API = "https://vipergirls.to/vr.php";

    private final AppStateExchange appStateExchange;
    private final VipergirlsAuthService authService;
    private final AppSettingsService appSettingsService;
    private final DownloadQ downloadQ;

    @Autowired
    public PostParser(AppStateExchange appStateExchange, VipergirlsAuthService authService, AppSettingsService appSettingsService, DownloadQ downloadQ) {
        this.appStateExchange = appStateExchange;
        this.authService = authService;
        this.appSettingsService = appSettingsService;
        this.downloadQ = downloadQ;
    }

    public synchronized void addPost(String postId, String threadId) throws PostParseException {

        if (appStateExchange.getPosts().containsKey(postId)) {
            logger.warn(String.format("skipping %s, already loaded", postId));
            return;
        }

        VRPostParser vrPostParser = new VRPostParser(threadId, postId);
        Post post = vrPostParser.parse();

        authService.leaveThanks(post);
        if (appSettingsService.getSettings().getAutoStart()) {
            logger.debug("Auto start downloads option is enabled");
            logger.debug(String.format("Starting to enqueue %d jobs for %s", post.getImages().size(), post.getUrl()));
            post.setStatus(Post.Status.PENDING);
            try {
                downloadQ.enqueue(post);
            } catch (InterruptedException e) {
                logger.warn("Interruption was caught");
                Thread.currentThread().interrupt();
                return;
            }
            logger.debug(String.format("Done enqueuing jobs for %s", post.getUrl()));
        } else {
            post.setStatus(Post.Status.STOPPED);
            logger.debug("Auto start downloads option is disabled");
        }
    }

    public VRThreadParser createVRThreadParser(QueuedVGLink queuedVGLink) {
        return new VRThreadParser(queuedVGLink);
    }
}
