package tn.mnlr.vripper.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.Queued;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.domain.ApiThreadParser;
import tn.mnlr.vripper.services.domain.MultiPostItem;
import tn.mnlr.vripper.services.domain.tasks.AddPostRunnable;
import tn.mnlr.vripper.services.domain.tasks.AddQueuedRunnable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static tn.mnlr.vripper.jpa.domain.Event.Status.DONE;
import static tn.mnlr.vripper.jpa.domain.Event.Status.PROCESSING;

@Service
@Slf4j
public class PostService {

    private final DataService dataService;
    private final ThreadPoolService threadPoolService;
    private final MetadataService metadataService;
    private final IEventRepository eventRepository;
    private final LoadingCache<Queued, List<MultiPostItem>> cache;

    @Autowired
    public PostService(DataService dataService, ThreadPoolService threadPoolService, MetadataService metadataService, IEventRepository eventRepository) {
        this.dataService = dataService;
        this.threadPoolService = threadPoolService;
        this.metadataService = metadataService;
        this.eventRepository = eventRepository;

        CacheLoader<Queued, List<MultiPostItem>> loader = new CacheLoader<>() {
            @Override
            public List<MultiPostItem> load(@NonNull Queued multiPostItem) throws Exception {
                return new ApiThreadParser(multiPostItem).parse();
            }
        };

        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build(loader);
    }

    public void stopFetchingMetadata(Post post) {
        metadataService.stopFetchingMetadata(post);
    }

    public void processMultiPost(List<Queued> queuedList) {
        for (Queued queued : queuedList) {
            if (queued.getPostId() != null) {
                threadPoolService.getGeneralExecutor().submit(new AddPostRunnable(queued.getPostId(), queued.getThreadId()));
            } else {
                threadPoolService.getGeneralExecutor().submit(new AddQueuedRunnable(queued));
            }
        }
    }

    public List<MultiPostItem> get(Queued queued) {
        List<MultiPostItem> multiPostItems;
        multiPostItems = cache.getIfPresent(queued);
        if (multiPostItems == null) {
            Event event = new Event(Event.Type.QUEUED_CACHE_MISS, PROCESSING, LocalDateTime.now(), "Loading posts from " + queued.getLink());
            eventRepository.save(event);
            try {
                multiPostItems = cache.get(queued);
                event.setStatus(DONE);
                event.setMessage("Loaded " + multiPostItems.size() + " posts from " + queued.getLink());
                eventRepository.update(event);
            } catch (Exception e) {
                String error = String.format("Failed to parse link %s", queued.getLink());
                log.error(error, e);
                event.setStatus(Event.Status.ERROR);
                event.setMessage(error + "\n" + Utils.throwableToString(e));
                eventRepository.update(event);
                return null;
            }
        }
        return multiPostItems;
    }

    public void remove(String threadId) {
        dataService.removeQueueLink(threadId);
    }
}
