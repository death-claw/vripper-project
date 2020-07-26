package tn.mnlr.vripper.services.post;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.FailsafeException;
import org.apache.http.impl.execchain.RequestAbortedException;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.services.PostDataService;

@Slf4j
public class MetadataRunnable implements Runnable {

    @Getter
    private final Post post;

    private final MetadataCache metadataCache;
    private final PostDataService postDataService;

    public MetadataRunnable(Post post) {
        this.post = post;
        this.metadataCache = SpringContext.getBean(MetadataCache.class);
        this.postDataService = SpringContext.getBean(PostDataService.class);
    }

    @Override
    public void run() {
        try {
            Metadata metadata = metadataCache.get(post);
            postDataService.setMetadata(post, metadata);
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException || (e.getCause() instanceof FailsafeException && (e.getCause().getCause() instanceof InterruptedException || e.getCause().getCause() instanceof RequestAbortedException))) {
                return;
            }
            log.error(String.format("Failed to get metadata for postId %s", post.getPostId()), e);
        }
    }
}
