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
import tn.mnlr.vripper.services.AppSettingsService;
import tn.mnlr.vripper.services.AppStateService;
import tn.mnlr.vripper.services.ThumbnailGenerator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    @Autowired
    private DownloadQ downloadQ;

    @Autowired
    private AppSettingsService settings;

    @Autowired
    private AppStateService appStateService;

    @Autowired
    private ThumbnailGenerator thumbnailGenerator;

    private final AtomicInteger threadCount = new AtomicInteger();

    private ExecutorService executor = Executors.newFixedThreadPool(10);

    private Thread executionThread;

    private RetryPolicy<Object> retryPolicy;

    private List<DownloadJob> running = Collections.synchronizedList(new ArrayList<>());

    private Map<String, Future<?>> futures = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {

        retryPolicy = new RetryPolicy<>()
                .handleIf(e -> !(e instanceof InterruptedException))
                .withBackoff(10, 60, ChronoUnit.SECONDS)
                .withMaxRetries(4)
                .abortOn(InterruptedException.class)
                .onFailedAttempt(e -> logger.warn(String.format("#%d tries failed", e.getAttemptCount()), e.getLastFailure()));

        executionThread = new Thread(this::start, "Executor thread");
        executionThread.start();
    }

    @PreDestroy
    private void destroy() throws InterruptedException {
        logger.info("Shutting down ExecutionService");
        executionThread.interrupt();
        executor.shutdown();
        appStateService.getCurrentPosts().keySet().forEach(p -> {
            logger.debug(String.format("Stopping download jobs for %s", p));
            this.stop(p);
        });
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    public void stop(String postId) {
        List<DownloadJob> data = running
                .stream()
                .filter(e -> e.getImage().getPostId().equals(postId))
                .peek(e -> e.getImage().setStatus(Image.Status.STOPPED))
                .collect(Collectors.toList());
        logger.debug(String.format("Interrupting %d jobs for post id %s", data.size(), postId));

        data.forEach(e -> {
            futures.get(e.getImage().getUrl()).cancel(true);
            if(e.getImageFileData().getImageRequest() != null) {
                e.getImageFileData().getImageRequest().abort();
            }
            e.getImage().cleanup();
        });
    }

    private boolean canRun() {
        boolean canRun = threadCount.get() < settings.getMaxThreads();
        if (canRun && downloadQ.isNotPauseQ()) {
            threadCount.incrementAndGet();
            return true;
        }
        return false;
    }

    public void start() {
        while (!Thread.interrupted()) {
            if (canRun()) {
                DownloadJob take;
                try {
                    take = downloadQ.take();
                    if (take == null) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                DownloadJob finalTake = take;
                Runnable task = () -> {
                    running.add(finalTake);

                    Failsafe.with(retryPolicy)
                            .onFailure(e -> {
                                if (e.getFailure() instanceof InterruptedException || (e.getFailure() instanceof FailsafeException && e.getFailure().getCause() instanceof InterruptedException)) {
                                    logger.debug("Job successfully interrupted");
                                    return;
                                }
                                logger.error(String.format("Failed to download %s after %d tries", finalTake.getImage().getUrl(), e.getAttemptCount()), e.getFailure());
                                finalTake.getImage().setStatus(Image.Status.ERROR);
                            })
                            .onComplete(e -> {
                                appStateService.doneDownloadJob(finalTake.getImage());
                                logger.debug(String.format("Finished downloading %s", finalTake.getImage().getUrl()));
                                VripperApplication.commonExecutor.submit(() -> thumbnailGenerator.getThumbnails().get(new ThumbnailGenerator.CacheKey(finalTake.getImage().getPostId(), finalTake.getImageFileData().getFileName())));
                                synchronized (threadCount) {
                                    threadCount.decrementAndGet();
                                    running.remove(finalTake);
                                    futures.remove(finalTake.getImage().getUrl());
                                    threadCount.notify();
                                }
                            })
                            .get(finalTake::call);
                };
                logger.debug(String.format("Scheduling a job for %s", finalTake.getImage().getUrl()));
                futures.put(finalTake.getImage().getUrl(), executor.submit(task));
            } else {
                synchronized (threadCount) {
                    try {
                        threadCount.wait(2_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    public int runningCount() {
        return running.size();
    }
}
