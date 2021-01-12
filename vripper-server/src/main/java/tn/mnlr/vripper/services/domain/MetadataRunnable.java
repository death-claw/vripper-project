package tn.mnlr.vripper.services.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.FailsafeException;
import org.apache.http.impl.execchain.RequestAbortedException;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.jpa.domain.Metadata;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.MetadataService;

@Slf4j
public class MetadataRunnable implements Runnable {

    @Getter
    private final Post post;

    private final MetadataService metadataService;
    private final DataService dataService;

    public MetadataRunnable(@NonNull Post post) {
        this.post = post;
        this.metadataService = SpringContext.getBean(MetadataService.class);
        this.dataService = SpringContext.getBean(DataService.class);
    }

    @Override
    public void run() {
        try {
            if (Thread.interrupted()) {
                log.debug(String.format("Metadata fetching for postId=%s, threadId=%s interrupted", post.getPostId(), post.getThreadId()));
                return;
            }
            Metadata metadata = metadataService.get(post);
            dataService.setMetadata(post, metadata);
        } catch (Exception e) {
            if (e.getCause() instanceof InterruptedException || (e.getCause() instanceof FailsafeException && (e.getCause().getCause() instanceof InterruptedException || e.getCause().getCause() instanceof RequestAbortedException))) {
                return;
            }
            log.error(String.format("Failed to get metadata for postId %s", post.getPostId()), e);
        }
    }
}
