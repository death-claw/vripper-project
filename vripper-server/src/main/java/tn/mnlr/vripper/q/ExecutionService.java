package tn.mnlr.vripper.q;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PostDataService;
import tn.mnlr.vripper.services.post.PostService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ExecutionService {

    private static final List<Status> FINISHED = Arrays.asList(Status.ERROR, Status.COMPLETE, Status.STOPPED);
    private final int MAX_POOL_SIZE = 12;

    private final ConcurrentHashMap<Host, AtomicInteger> threadCount = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_POOL_SIZE);
    private final BlockingQueue<DownloadJob> executionQueue = new LinkedBlockingQueue<>();
    private final Map<DownloadJob, Future<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> downloadCount = new ConcurrentHashMap<>();

    private final PendingQ pendingQ;
    private final AppSettingsService settings;
    private final PostDataService postDataService;
    private final PostService postService;

    private boolean pauseQ = false;
    private Thread executionThread;
    private Thread pollThread;

    @Autowired
    public ExecutionService(PendingQ pendingQ, AppSettingsService settings, PostDataService postDataService, PostService postService) {
        this.pendingQ = pendingQ;
        this.settings = settings;
        this.postDataService = postDataService;
        this.postService = postService;
    }

    @PostConstruct
    private void init() {
        executionThread = new Thread(this::start, "Executor thread");
        pollThread = new Thread(this::poll, "Polling thread");
        pollThread.start();
        executionThread.start();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        log.info("Shutting down ExecutionService");
        executionThread.interrupt();
        pollThread.interrupt();
        executor.shutdown();
        postDataService.findAllPosts().forEach(p -> {
            log.debug(String.format("Stopping download jobs for %s", p));
            this.stopRunning(p.getPostId());
        });
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private void stopRunning(@NonNull String postId) {
        futures
                .entrySet()
                .stream()
                .filter(e -> e.getKey().getImage().getPostId().equals(postId))
                .forEach(e -> {
                    e.getValue().cancel(true);
                    if (e.getKey().getImageFileData().getImageRequest() != null) {
                        e.getKey().getImageFileData().getImageRequest().abort();
                    }
                    e.getKey().getImage().setStatus(Status.STOPPED);
                    postDataService.updateImageStatus(e.getKey().getImage().getStatus(), e.getKey().getImage().getId());
                });
    }

    public void stopAll(List<String> posIds) {
        if (posIds != null) {
            posIds.forEach(this::stop);
        } else {
            postDataService.findAllPosts().forEach(p -> this.stop(p.getPostId()));
        }
    }

    public void restartAll(List<String> posIds) {
        if (posIds != null) {
            posIds.forEach(this::restart);
        } else {
            postDataService.findAllPosts().forEach(p -> this.restart(p.getPostId()));
        }
    }

    private void restart(@NonNull String postId) {
        if (isRunning(postId) || isPending(postId)) {
            log.warn(String.format("Cannot restart, jobs are currently running for post id %s", postId));
            return;
        }
        List<Image> images = postDataService.findByPostIdAndIsNotCompleted(postId);
        if (images.isEmpty()) {
            return;
        }
        Post post = postDataService.findPostByPostId(postId).orElseThrow();
        post.setStatus(Status.PENDING);
        postDataService.updatePostStatus(post.getStatus(), post.getId());
        log.debug(String.format("Restarting %d jobs for post id %s", images.size(), postId));
        for (Image image : images) {
            try {
                pendingQ.put(post, image);
            } catch (InterruptedException e) {
                log.warn("Thread was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isPending(String postId) {
        return pendingQ.isPending(postId);
    }

    public boolean isRunning(@NonNull final String postId) {
        AtomicInteger runningCount = downloadCount.get(postId);
        return runningCount != null && runningCount.get() > 0;
    }

    private void stop(String postId) {
        try {
            pauseQ = true;
            final Post post = postDataService.findPostByPostId(postId).orElseThrow();
            if (post == null) {
                return;
            }
            if (FINISHED.contains(post.getStatus())) {
                return;
            }
            pendingQ.remove(post);
            stopRunning(postId);
            postDataService.stopImagesByPostIdAndIsNotCompleted(postId);
            postService.stopFetchingMetadata(post);
        } finally {
            pauseQ = false;
        }
    }

    private boolean canRun(Host host) {
        boolean canRun;
        AtomicInteger count = threadCount.get(host);
        if (count == null) {
            threadCount.put(host, new AtomicInteger(0));
        }
        canRun = threadCount.get(host).get() < settings.getSettings().getMaxThreads() && (settings.getSettings().getMaxTotalThreads() == 0 ? threadCount.values().stream().mapToInt(AtomicInteger::get).sum() < MAX_POOL_SIZE : threadCount.values().stream().mapToInt(AtomicInteger::get).sum() < settings.getSettings().getMaxTotalThreads());
        if (canRun && !pauseQ) {
            threadCount.get(host).incrementAndGet();
            return true;
        }
        return false;
    }

    private void poll() {
        while (!Thread.interrupted()) {
            try {
                List<DownloadJob> peek = pendingQ.peek();
                for (DownloadJob downloadJob : peek) {
                    if (canRun(downloadJob.getImage().getHost())) {
                        executionQueue.offer(downloadJob);
                        pendingQ.remove(downloadJob);
                    }
                }
                synchronized (threadCount) {
                    threadCount.wait(2_000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void start() {
        while (!Thread.interrupted()) {
            try {
                push(executionQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Execution Service failed", e);
                break;
            }
        }
    }

    private void push(DownloadJob downloadJob) {
        ExecuteRunnable runnable = new ExecuteRunnable(downloadJob);
        log.debug(String.format("Scheduling a job for %s", downloadJob.getImage().getUrl()));
        futures.put(downloadJob, executor.submit(runnable));
    }

    public void beforeJobStart(String postId) {
        checkKeyRunningPosts(postId);
        downloadCount.get(postId).incrementAndGet();
    }

    private synchronized void checkKeyRunningPosts(@NonNull final String postId) {
        if (!downloadCount.containsKey(postId)) {
            downloadCount.put(postId, new AtomicInteger(0));
        }
    }

    public synchronized void afterJobFinish(DownloadJob downloadJob) {
        int count = downloadCount.get(downloadJob.getPost().getPostId()).decrementAndGet();
        if (count == 0) {
            downloadCount.remove(downloadJob.getPost().getPostId());
            postDataService.finishPost(downloadJob.getPost());
        }
        threadCount.get(downloadJob.getImage().getHost()).decrementAndGet();
        futures.remove(downloadJob);
        synchronized (threadCount) {
            threadCount.notify();
        }
    }

    public int runningCount() {
        return futures.size();
    }
}
