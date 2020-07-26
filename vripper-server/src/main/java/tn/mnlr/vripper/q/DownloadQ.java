package tn.mnlr.vripper.q;

import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PostDataService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DownloadQ {

    private static final Logger logger = LoggerFactory.getLogger(DownloadQ.class);

    private final PostDataService postDataService;
    private final AppSettingsService appSettingsService;
    private final List<Host> hosts;

    private final ConcurrentHashMap<Host, BlockingDeque<DownloadJob>> downloadQ = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, AtomicInteger> downloading = new ConcurrentHashMap<>();

    @Autowired
    public DownloadQ(PostDataService postDataService, AppSettingsService appSettingsService, List<Host> hosts) {
        this.postDataService = postDataService;
        this.appSettingsService = appSettingsService;
        this.hosts = hosts;
    }

    @PostConstruct
    private void init() {
        hosts.forEach(host -> downloadQ.put(host, new LinkedBlockingDeque<>()));
    }

    public void put(Post post, Image image) throws InterruptedException {
        logger.debug(String.format("Enqueuing a job for %s", image.getUrl()));
        image.init();
        postDataService.updateImageStatus(image.getStatus(), image.getId());
        postDataService.updateImageCurrent(image.getCurrent(), image.getId());
        DownloadJob downloadJob = new DownloadJob(post, image);
        downloadQ.get(downloadJob.getImage().getHost()).putLast(downloadJob);
        beforeJobStart(post.getPostId());
    }

    public void beforeJobStart(String postId) {
        checkKeyRunningPosts(postId);
        downloading.get(postId).incrementAndGet();
    }

    private synchronized void checkKeyRunningPosts(@NonNull final String postId) {
        if (!downloading.containsKey(postId)) {
            downloading.put(postId, new AtomicInteger(0));
        }
    }

    public void remove(final DownloadJob downloadJob) {
        downloadQ.get(downloadJob.getImage().getHost()).remove(downloadJob);
    }

    public List<DownloadJob> peek() {
        List<DownloadJob> downloadJobs = new ArrayList<>();
        if (hosts.size() == 0) {
            return downloadJobs;
        }
        for (Host host : hosts) {
            Iterator<DownloadJob> it = downloadQ.get(host).iterator();
            for (int i = 0; i < appSettingsService.getSettings().getMaxThreads(); i++) {
                DownloadJob downloadJob = it.hasNext() ? it.next() : null;
                if (downloadJob != null) {
                    downloadJobs.add(downloadJob);
                }
            }
        }
        return downloadJobs;
    }

    public void enqueue(Post post, Set<Image> images) throws InterruptedException {
        for (Image image : images) {
            put(post, image);
        }
    }

    public int size() {
        return downloadQ.values().stream().mapToInt(BlockingDeque::size).sum();
    }

    public Iterable<? extends Map.Entry<Host, BlockingDeque<DownloadJob>>> entries() {
        return downloadQ.entrySet();
    }
}
