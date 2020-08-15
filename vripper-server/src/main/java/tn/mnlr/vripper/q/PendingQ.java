package tn.mnlr.vripper.q;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.DataService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Service
@Slf4j
public class PendingQ {

    private final DataService dataService;
    private final AppSettingsService appSettingsService;
    private final List<Host> hosts;

    private final ConcurrentHashMap<Host, BlockingDeque<DownloadJob>> pendingQ = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> toBeExecuted = new ConcurrentHashMap<>();

    @Autowired
    public PendingQ(DataService dataService, AppSettingsService appSettingsService, List<Host> hosts) {
        this.dataService = dataService;
        this.appSettingsService = appSettingsService;
        this.hosts = hosts;
    }

    @PostConstruct
    private void init() {
        hosts.forEach(host -> pendingQ.put(host, new LinkedBlockingDeque<>()));
    }

    public void put(Post post, Image image) throws InterruptedException {
        log.debug(String.format("Enqueuing a job for %s", image.getUrl()));
        image.init();
        dataService.updateImageStatus(image.getStatus(), image.getId());
        dataService.updateImageCurrent(image.getCurrent(), image.getId());
        DownloadJob downloadJob = new DownloadJob(post, image);
        pendingQ.get(downloadJob.getImage().getHost()).putLast(downloadJob);
        checkKey(post.getPostId());
        toBeExecuted.get(post.getPostId()).incrementAndGet();
    }

    private synchronized void checkKey(String postId) {
        if (!toBeExecuted.containsKey(postId)) {
            toBeExecuted.put(postId, new AtomicInteger(0));
        }
    }

    public void remove(final DownloadJob downloadJob) {
        pendingQ.get(downloadJob.getImage().getHost()).remove(downloadJob);
    }

    public List<DownloadJob> peek() {
        List<DownloadJob> downloadJobs = new ArrayList<>();
        if (hosts.size() == 0) {
            return downloadJobs;
        }
        for (Host host : hosts) {
            Iterator<DownloadJob> it = pendingQ.get(host).iterator();
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
        return toBeExecuted.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public void stop(Post post) {
        Predicate<DownloadJob> predicate = next -> next.getImage().getPostId().equals(post.getPostId());
        for (Map.Entry<Host, BlockingDeque<DownloadJob>> entry : pendingQ.entrySet()) {
            entry.getValue().stream().filter(predicate).forEach(e -> decrement(post.getPostId()));
            entry.getValue().removeIf(predicate);
            decrement(post.getPostId());
        }
    }

    public boolean isPending(String postId) {
        return toBeExecuted.containsKey(postId);
    }

    public synchronized int decrement(String postId) {
        AtomicInteger counter = toBeExecuted.get(postId);
        if (counter == null) {
            return 0;
        }
        int count = counter.decrementAndGet();
        if (count == 0) {
            toBeExecuted.remove(postId);
        }
        return count;
    }
}
