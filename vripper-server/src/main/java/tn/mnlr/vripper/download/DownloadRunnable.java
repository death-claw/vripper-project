package tn.mnlr.vripper.download;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.Event;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.IEventRepository;
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.DataService;

import java.time.LocalDateTime;

@Slf4j
public class DownloadRunnable implements Runnable {

    private final DownloadService downloadService;
    private final DataService dataService;
    private final ConnectionService connectionService;
    private final IEventRepository eventRepository;

    private final DownloadJob downloadJob;

    public DownloadRunnable(final DownloadJob downloadJob) {
        downloadService = SpringContext.getBean(DownloadService.class);
        dataService = SpringContext.getBean(DataService.class);
        connectionService = SpringContext.getBean(ConnectionService.class);
        eventRepository = SpringContext.getBean(IEventRepository.class);

        this.downloadJob = downloadJob;
    }

    @Override
    public void run() {
        Failsafe.with(connectionService.getRetryPolicy())
                .onFailure(e -> {
                    try {
                        Event event = new Event(Event.Type.DOWNLOAD, Event.Status.ERROR, LocalDateTime.now(), String.format("Failed to download %s\n %s", downloadJob.getImage().getUrl(), Utils.throwableToString(e.getFailure())));
                        eventRepository.save(event);
                    } catch (Exception exp) {
                        log.error("Failed to save event", exp);
                    }
                    log.error(String.format("Failed to download %s after %d tries", downloadJob.getImage().getUrl(), e.getAttemptCount()), e.getFailure());
                    downloadJob.getImage().setStatus(Status.ERROR);
                    dataService.updateImageStatus(downloadJob.getImage().getStatus(), downloadJob.getImage().getId());
                })
                .onComplete(e -> {
                    dataService.afterJobFinish(downloadJob.getImage(), downloadJob.getPost());
                    downloadService.afterJobFinish(downloadJob);
                    log.debug(String.format("Finished downloading %s", downloadJob.getImage().getUrl()));
                }).run(downloadJob);
    }
}
