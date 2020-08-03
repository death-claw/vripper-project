package tn.mnlr.vripper.q;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.services.DataService;
import tn.mnlr.vripper.services.MutexService;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ExecuteRunnable implements Runnable {

    private final ExecutionService executionService;
    private final DataService dataService;
    private final MutexService mutexService;

    private final DownloadJob downloadJob;

    public ExecuteRunnable(final DownloadJob downloadJob) {
        executionService = SpringContext.getBean(ExecutionService.class);
        dataService = SpringContext.getBean(DataService.class);
        mutexService = SpringContext.getBean(MutexService.class);
        this.downloadJob = downloadJob;
    }

    @Override
    public void run() {

        mutexService.createPostLock(downloadJob.getPost().getPostId());
        ReentrantLock mutex = mutexService.getPostLock(downloadJob.getPost().getPostId());
        mutex.lock();
        Failsafe.with(VripperApplication.retryPolicy)
                .onFailure(e -> {
                    if (e.getFailure() instanceof InterruptedException || e.getFailure().getCause() instanceof InterruptedException) {
                        log.debug("Job successfully interrupted");
                        return;
                    }
                    log.error(String.format("Failed to download %s after %d tries", downloadJob.getImage().getUrl(), e.getAttemptCount()), e.getFailure());
                    downloadJob.getImage().setStatus(Status.ERROR);
                    dataService.updateImageStatus(downloadJob.getImage().getStatus(), downloadJob.getImage().getId());
                })
                .onComplete(e -> {
                    mutex.unlock();
                    dataService.afterJobFinish(downloadJob.getImage(), downloadJob.getPost());
                    executionService.afterJobFinish(downloadJob);
                    log.debug(String.format("Finished downloading %s", downloadJob.getImage().getUrl()));
                }).run(downloadJob);
    }
}
