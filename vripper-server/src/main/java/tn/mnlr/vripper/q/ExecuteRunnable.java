package tn.mnlr.vripper.q;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.VripperApplication;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.services.PostDataService;

@Slf4j
public class ExecuteRunnable implements Runnable {

    private final ExecutionService executionService;
    private final PostDataService postDataService;
    private final DownloadJob downloadJob;

    public ExecuteRunnable(final DownloadJob downloadJob) {
        executionService = SpringContext.getBean(ExecutionService.class);
        postDataService = SpringContext.getBean(PostDataService.class);
        this.downloadJob = downloadJob;
    }

    @Override
    public void run() {

        executionService.beforeJobStart(downloadJob.getPost().getPostId());
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
                    executionService.afterJobFinish(downloadJob);
                    log.info(String.format("Finished downloading %s", downloadJob.getImage().getUrl()));
                }).run(downloadJob);
    }
}
