package tn.mnlr.vripper.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.tasks.MetadataRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MetadataService {

  private final Map<String, MetadataRunnable> fetchingMetadata = new ConcurrentHashMap<>();
  private final ThreadPoolService threadPoolService;

  public MetadataService(ThreadPoolService threadPoolService) {
    this.threadPoolService = threadPoolService;
  }

  public void startFetchingMetadata(Post post) {
    MetadataRunnable runnable = new MetadataRunnable(post);
    threadPoolService.getGeneralExecutor().submit(runnable);
    fetchingMetadata.put(post.getPostId(), runnable);
  }

  public void stopFetchingMetadata(List<String> postIds) {
    List<MetadataRunnable> stopping = new ArrayList<>();

    for (Map.Entry<String, MetadataRunnable> entry : this.fetchingMetadata.entrySet()) {
      if (postIds.contains(entry.getKey())) {
        stopping.add(entry.getValue());
        entry.getValue().stop();
      }
    }

    while (!stopping.isEmpty()) {
      stopping.removeIf(MetadataRunnable::isFinished);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    postIds.forEach(fetchingMetadata::remove);
  }
}
