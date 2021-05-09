package tn.mnlr.vripper.download;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.Utils;
import tn.mnlr.vripper.jpa.domain.LogEvent;
import tn.mnlr.vripper.jpa.domain.enums.Status;
import tn.mnlr.vripper.jpa.repositories.ILogEventRepository;
import tn.mnlr.vripper.services.ConnectionService;
import tn.mnlr.vripper.services.DataService;

import java.time.LocalDateTime;

@Slf4j
public class DownloadJobWrapper implements Runnable {

  private final DownloadService downloadService;
  private final DataService dataService;
  private final ConnectionService connectionService;
  private final ILogEventRepository eventRepository;

  private final DownloadJob downloadJob;

  public DownloadJobWrapper(final DownloadJob downloadJob) {
    downloadService = SpringContext.getBean(DownloadService.class);
    dataService = SpringContext.getBean(DataService.class);
    connectionService = SpringContext.getBean(ConnectionService.class);
    eventRepository = SpringContext.getBean(ILogEventRepository.class);

    this.downloadJob = downloadJob;
  }

  @Override
  public void run() {
    Failsafe.with(connectionService.getRetryPolicy())
        .onFailure(
            e -> {
              try {
                LogEvent logEvent =
                    new LogEvent(
                        LogEvent.Type.DOWNLOAD,
                        LogEvent.Status.ERROR,
                        LocalDateTime.now(),
                        String.format(
                            "Failed to download %s\n %s",
                            downloadJob.getImage().getUrl(),
                            Utils.throwableToString(e.getFailure())));
                eventRepository.save(logEvent);
              } catch (Exception exp) {
                log.error("Failed to save event", exp);
              }
              log.error(
                  String.format(
                      "Failed to download %s after %d tries",
                      downloadJob.getImage().getUrl(), e.getAttemptCount()),
                  e.getFailure());
              downloadJob.getImage().setStatus(Status.ERROR);
              dataService.updateImageStatus(
                  downloadJob.getImage().getStatus(), downloadJob.getImage().getId());
            })
        .onComplete(
            e -> {
              dataService.afterJobFinish(downloadJob.getImage(), downloadJob.getPost());
              downloadService.afterJobFinish(downloadJob);
              log.debug(String.format("Finished downloading %s", downloadJob.getImage().getUrl()));
            })
        .run(downloadJob);
  }
}
