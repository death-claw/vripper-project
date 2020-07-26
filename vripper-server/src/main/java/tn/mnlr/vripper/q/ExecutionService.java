package tn.mnlr.vripper.q;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.jpa.domain.Image;
import tn.mnlr.vripper.jpa.domain.Post;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.PostDataService;
import tn.mnlr.vripper.services.post.PostService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExecutionService {

    private static final List<Status> FINISHED = Arrays.asList(Status.ERROR, Status.COMPLETE, Status.STOPPED);

    private final DownloadQ downloadQ;
    private final AppSettingsService settings;
    private final PostDataService postDataService;
    private final PostService postService;

    private final int MAX_POOL_SIZE = 12;

    private final ConcurrentHashMap<Host, AtomicInteger> threadCount = new ConcurrentHashMap<>();

    private boolean pauseQ = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_POOL_SIZE);

    private Thread executionThread;

    private final BlockingQueue<DownloadJob> queue = new LinkedBlockingQueue<>();

    private final List<DownloadJob> running = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, Future<?>> futures = new ConcurrentHashMap<>();
    private Thread pollThread;

    @Autowired
    public ExecutionService(DownloadQ downloadQ, AppSettingsService settings, PostDataService postDataService, PostService postService) {
        this.downloadQ = downloadQ;
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
        List<DownloadJob> data = running
                .stream()
                .filter(e -> e.getImage().getPostId().equals(postId))
                .collect(Collectors.toList());
        log.debug(String.format("Interrupting %d jobs for post id %s", data.size(), postId));

        data.forEach(e -> {
            futures.get(e.getImage().getUrl()).cancel(true);
            if (e.getImageFileData().getImageRequest() != null) {
                e.getImageFileData().getImageRequest().abort();
            }
            e.getImage().setStatus(Status.STOPPED);
            postDataService.updateImageStatus(e.getImage().getStatus(), e.getImage().getId());
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
        if (isRunning(postId)) {
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
                downloadQ.put(post, image);
            } catch (InterruptedException e) {
                log.warn("Thread was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning(@NonNull final String postId) {
        AtomicInteger runningCount = downloadQ.getDownloading().get(postId);
        return runningCount != null && runningCount.get() > 0;
    }

    private void removeScheduled(Post post) {
        for (Map.Entry<Host, BlockingDeque<DownloadJob>> entry : downloadQ.entries()) {
            entry.getValue().removeIf(next -> next.getImage().getPostId().equals(post.getPostId()));
        }
        postDataService.finishPost(post);
    }

    private void stop(String postId) {
        try {
            downloadQ.getDownloading().remove(postId);
            pauseQ = true;
            final Post post = postDataService.findPostByPostId(postId).orElseThrow();
            if (post == null) {
                return;
            }
            if (FINISHED.contains(post.getStatus())) {
                return;
            }
            removeScheduled(post);
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
                List<DownloadJob> peek = downloadQ.peek();
                for (DownloadJob downloadJob : peek) {
                    if (canRun(downloadJob.getImage().getHost())) {
                        queue.offer(downloadJob);
                        downloadQ.remove(downloadJob);
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
                push(queue.take());
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
        Runnable task = () -> {
            running.add(downloadJob);

            Failsafe.with(VripperApplication.retryPolicy)
                    .onFailure(e -> {
                        if (e.getFailure() instanceof InterruptedException || e.getFailure().getCause() instanceof InterruptedException) {
                            log.debug("Job successfully interrupted");
                            return;
                        }
                        log.error(String.format("Failed to download %s after %d tries", downloadJob.getImage().getUrl(), e.getAttemptCount()), e.getFailure());
                        downloadJob.getImage().setStatus(Status.ERROR);
                        postDataService.updateImageStatus(downloadJob.getImage().getStatus(), downloadJob.getImage().getId());
                    })
                    .onComplete(e -> {
                        postDataService.afterJobFinish(downloadJob.getImage(), downloadJob.getPost());
                        if (downloadQ.getDownloading().get(downloadJob.getPost().getPostId()).decrementAndGet() == 0) {
                            downloadQ.getDownloading().remove(downloadJob.getPost().getPostId());
                            postDataService.finishPost(downloadJob.getPost());
                        }
                        log.debug(String.format("Finished downloading %s", downloadJob.getImage().getUrl()));
                        threadCount.get(downloadJob.getImage().getHost()).decrementAndGet();
                        running.remove(downloadJob);
                        futures.remove(downloadJob.getImage().getUrl());
                        synchronized (threadCount) {
                            threadCount.notify();
                        }
                    }).run(downloadJob);
        };
        log.debug(String.format("Scheduling a job for %s", downloadJob.getImage().getUrl()));
        futures.put(downloadJob.getImage().getUrl(), executor.submit(task));
    }

    public int runningCount() {
        return running.size();
    }
}
