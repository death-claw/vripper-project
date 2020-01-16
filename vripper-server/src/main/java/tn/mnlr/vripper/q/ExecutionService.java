package tn.mnlr.vripper.q;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.host.Host;
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.AppStateService;
import tn.mnlr.vripper.services.ThumbnailGenerator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private static final List<Post.Status> FINISHED = Arrays.asList(Post.Status.ERROR, Post.Status.COMPLETE, Post.Status.STOPPED);

    @Autowired
    private DownloadQ downloadQ;

    @Autowired
    private AppSettingsService settings;

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private ThumbnailGenerator thumbnailGenerator;

    @Autowired
    private AppSettingsService appSettingsService;
    private final ConcurrentHashMap<Host, AtomicInteger> threadCount = new ConcurrentHashMap<>();
    private boolean notPauseQ = true;
    private ExecutorService executor = Executors.newFixedThreadPool(20);

    private Thread executionThread;

    BlockingQueue<DownloadJob> queue = new LinkedBlockingQueue<>();

    private RetryPolicy<Object> retryPolicy;

    private List<DownloadJob> running = Collections.synchronizedList(new ArrayList<>());

    private Map<String, Future<?>> futures = new ConcurrentHashMap<>();
    private Thread pollThread;

    @PostConstruct
    private void init() {

        retryPolicy = new RetryPolicy<>()
                .handleIf(e -> !(e instanceof InterruptedException))
                .withBackoff(1, 10, ChronoUnit.SECONDS)
                .withMaxDuration(Duration.of(10, ChronoUnit.SECONDS))
                .withMaxAttempts(3)
                .abortOn(InterruptedException.class)
                .onFailedAttempt(e -> logger.warn(String.format("#%d tries failed", e.getAttemptCount()), e.getLastFailure()));

        executionThread = new Thread(this::start, "Executor thread");
        pollThread = new Thread(this::poll, "Polling thread");
        pollThread.start();
        executionThread.start();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        logger.info("Shutting down ExecutionService");
        executionThread.interrupt();
        executor.shutdown();
        appStateService.getCurrentPosts().keySet().forEach(p -> {
            logger.debug(String.format("Stopping download jobs for %s", p));
            this.stopRunning(p);
        });
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    public void stopRunning(String postId) {
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


    public synchronized void stopAll() {
        appStateService.getCurrentPosts().values().stream().map(Post::getPostId).forEach(this::stop);
    }

    public synchronized void restartAll() throws InterruptedException {
        for (Post post : appStateService.getCurrentPosts().values()) {
            String postId = post.getPostId();
            restart(postId);
        }
    }

    public synchronized void restart(String postId) throws InterruptedException {
        if (appStateService.getRunningPosts().get(postId) != null && appStateService.getRunningPosts().get(postId).get() > 0) {
            logger.warn(String.format("Cannot restart, jobs are currently running for post id %s", postId));
            return;
        }
        List<Image> images = appStateService.getPost(postId)
                .getImages()
                .stream()
                .filter(e -> !e.getStatus().equals(Image.Status.COMPLETE))
                .collect(Collectors.toList());
        if (images.isEmpty()) {
            return;
        }
        appStateService.getPost(postId).setStatus(Post.Status.PENDING);
        logger.debug(String.format("Restarting %d jobs for post id %s", images.size(), postId));
        for (Image image : images) {
            downloadQ.put(image);
        }
    }

    private synchronized void removeScheduled(Image image) {
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

    private synchronized void removeRunning(String postId) {
        logger.debug(String.format("Interrupting running jobs for post id %s", postId));
        stopRunning(postId);
    }


    public synchronized void stop(String postId) {
        try {
            if (FINISHED.contains(appStateService.getPost(postId).getStatus())) {
                return;
            }
            notPauseQ = false;
            appStateService.getPost(postId).setStatus(Post.Status.STOPPED);
            List<Image> images = appStateService.getPost(postId)
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
        canRun = threadCount.get(host).get() < settings.getMaxThreads() && threadCount.values().stream().mapToInt(AtomicInteger::get).sum() < settings.getMaxTotalThreads();
        if (canRun && notPauseQ) {
            threadCount.get(host).incrementAndGet();
            return true;
        }
        return false;
    }

    public void poll() {
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

    public void start() {
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

            Failsafe.with(retryPolicy)
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
                        if (appSettingsService.isViewPhotos()) {
                            VripperApplication.commonExecutor.submit(
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
