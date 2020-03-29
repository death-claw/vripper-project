package tn.mnlr.vripper.q;

import lombok.NonNull;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);
    private static final List<Post.Status> FINISHED = Arrays.asList(Post.Status.ERROR, Post.Status.COMPLETE, Post.Status.STOPPED);

    private final DownloadQ downloadQ;
    private final AppSettingsService settings;
    private final AppStateService appStateService;
    private final AppStateExchange appStateExchange;
    private final ThumbnailGenerator thumbnailGenerator;
    private final AppSettingsService appSettingsService;
    private final CommonExecutor commonExecutor;

    private final int MAX_POOL_SIZE = 12;

    private final ConcurrentHashMap<Host, AtomicInteger> threadCount = new ConcurrentHashMap<>();

    private boolean notPauseQ = true;
    private ExecutorService executor = Executors.newFixedThreadPool(MAX_POOL_SIZE);

    private Thread executionThread;

    private BlockingQueue<DownloadJob> queue = new LinkedBlockingQueue<>();

    private List<DownloadJob> running = Collections.synchronizedList(new ArrayList<>());

    private Map<String, Future<?>> futures = new ConcurrentHashMap<>();
    private Thread pollThread;

    @Autowired
    public ExecutionService(DownloadQ downloadQ, AppSettingsService settings, AppStateService appStateService, AppStateExchange appStateExchange, ThumbnailGenerator thumbnailGenerator, AppSettingsService appSettingsService, CommonExecutor commonExecutor) {
        this.downloadQ = downloadQ;
        this.settings = settings;
        this.appStateService = appStateService;
        this.appStateExchange = appStateExchange;
        this.thumbnailGenerator = thumbnailGenerator;
        this.appSettingsService = appSettingsService;
        this.commonExecutor = commonExecutor;
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
        logger.info("Shutting down ExecutionService");
        executionThread.interrupt();
        pollThread.interrupt();
        executor.shutdown();
        appStateExchange.getPosts().keySet().forEach(p -> {
            logger.debug(String.format("Stopping download jobs for %s", p));
            this.stopRunning(p);
        });
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    private void stopRunning(@NonNull String postId) {
        List<DownloadJob> data = running
                .stream()
                .filter(e -> e.getImage().getPostId().equals(postId))
                .peek(e -> e.getImage().setStatus(Image.Status.STOPPED))
                .collect(Collectors.toList());
        logger.debug(String.format("Interrupting %d jobs for post id %s", data.size(), postId));

        data.forEach(e -> {
            futures.get(e.getImage().getUrl()).cancel(true);
            if (e.getImageFileData().getImageRequest() != null) {
                e.getImageFileData().getImageRequest().abort();
            }
            e.getImage().cleanup();
        });
    }


    public synchronized void stopAll(List<String> posIds) {
        if (posIds != null) {
            posIds.forEach(this::stop);
        } else {
            appStateExchange.getPosts().values().forEach(p -> this.stop(p.getPostId()));
        }
    }

    public synchronized void restartAll(List<String> posIds) {
        if (posIds != null) {
            posIds.forEach(this::restart);
        } else {
            appStateExchange.getPosts().values().forEach(p -> this.restart(p.getPostId()));
        }
    }

    private void restart(@NonNull String postId) {
        if (appStateService.isRunning(postId)) {
            logger.warn(String.format("Cannot restart, jobs are currently running for post id %s", postId));
            return;
        }
        Post post = appStateExchange.getPost(postId);
        List<Image> images = post
                .getImages()
                .stream()
                .filter(e -> !e.getStatus().equals(Image.Status.COMPLETE))
                .collect(Collectors.toList());
        if (images.isEmpty()) {
            return;
        }

        post.setStatus(Post.Status.PENDING);
        logger.debug(String.format("Restarting %d jobs for post id %s", images.size(), postId));
        for (Image image : images) {
            try {
                downloadQ.put(post, image);
            } catch (InterruptedException e) {
                logger.warn("Thread was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void removeScheduled(Image image) {
        image.setStatus(Image.Status.STOPPED);
        logger.debug(String.format("Removing scheduled job for %s", image.getUrl()));

        boolean removed = false;
        main:
        for (Map.Entry<Host, BlockingDeque<DownloadJob>> entry : downloadQ.entries()) {
            Iterator<DownloadJob> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                DownloadJob next = iterator.next();
                if (next.getImage().getPostId().equals(image.getPostId())) {
                    iterator.remove();
                    appStateService.doneDownloadJob(image);
                    logger.debug(String.format("Scheduled job for %s is removed", image.getUrl()));
                    removed = true;
                    break main;
                }
            }
        }

        if (!removed) {
            logger.debug(String.format("Job for %s does not exist", image.getUrl()));
        }

        image.cleanup();
    }

    private void removeRunning(String postId) {
        logger.debug(String.format("Interrupting running jobs for post id %s", postId));
        stopRunning(postId);
    }

    private void stop(String postId) {
        try {
            final Post post = appStateExchange.getPost(postId);
            if (post == null) {
                return;
            }
            if (FINISHED.contains(post.getStatus())) {
                return;
            }
            notPauseQ = false;
            post.setStatus(Post.Status.STOPPED);
            List<Image> images = post
                    .getImages()
                    .stream()
                    .filter(e -> !e.getStatus().equals(Image.Status.COMPLETE))
                    .collect(Collectors.toList());
            if (images.isEmpty()) {
                return;
            }
            logger.debug(String.format("Stopping %d jobs for post id %s", images.size(), postId));
            images.forEach(this::removeScheduled);
            removeRunning(postId);
        } finally {
            notPauseQ = true;
        }
    }

    private boolean canRun(Host host) {
        boolean canRun;
        AtomicInteger count = threadCount.get(host);
        if (count == null) {
            threadCount.put(host, new AtomicInteger(0));
        }
        canRun = threadCount.get(host).get() < settings.getSettings().getMaxThreads() && (settings.getSettings().getMaxTotalThreads() == 0 ? threadCount.values().stream().mapToInt(AtomicInteger::get).sum() < MAX_POOL_SIZE : threadCount.values().stream().mapToInt(AtomicInteger::get).sum() < settings.getSettings().getMaxTotalThreads());
        if (canRun && notPauseQ) {
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
                logger.error("Execution Service failed", e);
                break;
            }
        }
    }

    private void push(DownloadJob take) {
        Runnable task = () -> {
            running.add(take);

            Failsafe.with(VripperApplication.retryPolicy)
                    .onFailure(e -> {
                        if (e.getFailure() instanceof InterruptedException || (e.getFailure() instanceof FailsafeException && e.getFailure().getCause() instanceof InterruptedException)) {
                            logger.debug("Job successfully interrupted");
                            return;
                        }
                        logger.error(String.format("Failed to download %s after %d tries", take.getImage().getUrl(), e.getAttemptCount()), e.getFailure());
                        take.getImage().setStatus(Image.Status.ERROR);
                    })
                    .onComplete(e -> {
                        appStateService.doneDownloadJob(take.getImage());
                        logger.debug(String.format("Finished downloading %s", take.getImage().getUrl()));
                        if (appSettingsService.getSettings().getViewPhotos()) {
                            commonExecutor.getGeneralExecutor().submit(
                                    () -> thumbnailGenerator.getThumbnails()
                                            .get(new ThumbnailGenerator.CacheKey(take.getImage().getPostId(), take.getImageFileData().getFileName())));
                        }
                        threadCount.get(take.getImage().getHost()).decrementAndGet();
                        running.remove(take);
                        futures.remove(take.getImage().getUrl());
                        synchronized (threadCount) {
                            threadCount.notify();
                        }
                    })
                    .get(take::call);
        };
        logger.debug(String.format("Scheduling a job for %s", take.getImage().getUrl()));
        futures.put(take.getImage().getUrl(), executor.submit(task));
    }

    public int runningCount() {
        return running.size();
    }
}
