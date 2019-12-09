package tn.mnlr.vripper.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.exception.PostParseException;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.q.DownloadQ;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class PostParser {

    private static final Logger logger = LoggerFactory.getLogger(PostParser.class);

    static final String VR_API = "https://vipergirls.to/vr.php";

    @Autowired
    private ConnectionManager cm;

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private VipergirlsAuthService authService;

    @Autowired
    private AppSettingsService appSettingsService;

    @Autowired
    private DownloadQ downloadQ;

    @Autowired
    private VipergirlsAuthService vipergirlsAuthService;

    @Autowired
    private List<Host> supportedHosts;

    @PostConstruct
    private void init() {

    }

    public void addPost(String postId, String threadId) throws PostParseException {

        if (appStateService.getCurrentPosts().containsKey(postId)) {
            logger.warn(String.format("skipping %s, already loaded", postId));
            return;
        }

        VRPostParser vrPostParser = new VRPostParser(threadId, postId, cm, vipergirlsAuthService, supportedHosts);
        Post post = vrPostParser.parse();

        post.setAppStateService(appStateService);
        post.getImages().forEach(e -> e.setAppStateService(appStateService));
        authService.leaveThanks(post.getUrl(), post.getPostId());
        if (appSettingsService.isAutoStart()) {
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

    public VRThreadParser createVRThreadParser(String threadId) {
        return new VRThreadParser(threadId, cm, vipergirlsAuthService, supportedHosts);
    }
}
