package tn.mnlr.vripper.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.services.domain.MultiPostScanParser;
import tn.mnlr.vripper.services.domain.MultiPostScanResult;
import tn.mnlr.vripper.services.domain.tasks.AddPostRunnable;
import tn.mnlr.vripper.services.domain.tasks.AddQueuedRunnable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PostService {

  private final DataService dataService;
  private final ThreadPoolService threadPoolService;
  private final MetadataService metadataService;
  private final LoadingCache<Queued, MultiPostScanResult> cache;

  @Autowired
  public PostService(
      DataService dataService,
      ThreadPoolService threadPoolService,
      MetadataService metadataService) {
    this.dataService = dataService;
    this.threadPoolService = threadPoolService;
    this.metadataService = metadataService;
    cache =
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(multiPostItem -> new MultiPostScanParser(multiPostItem).parse());
  }

  public void stopFetchingMetadata(Post post) {
    metadataService.stopFetchingMetadata(post);
  }

  public void processMultiPost(List<Queued> queuedList) {
    for (Queued queued : queuedList) {
      if (queued.getPostId() != null) {
        threadPoolService
            .getGeneralExecutor()
            .submit(new AddPostRunnable(queued.getPostId(), queued.getThreadId()));
      } else {
        threadPoolService.getGeneralExecutor().submit(new AddQueuedRunnable(queued));
      }
    }
  }

  public MultiPostScanResult get(Queued queued) throws ExecutionException {
    return cache.get(queued);
  }

  public void remove(String threadId) {
    dataService.removeQueueLink(threadId);
  }
}
