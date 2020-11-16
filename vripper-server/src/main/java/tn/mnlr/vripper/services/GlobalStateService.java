package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.download.DownloadService;
import tn.mnlr.vripper.download.PendingQueue;
import tn.mnlr.vripper.services.domain.GlobalState;

@Service
@EnableScheduling
public class GlobalStateService {

    private final PendingQueue pendingQueue;
    private final DownloadService downloadService;
    private final DataService dataService;

    @Getter
    private GlobalState currentState;

    @Getter
    private final PublishProcessor<GlobalState> liveGlobalState = PublishProcessor.create();

    @Autowired
    public GlobalStateService(PendingQueue pendingQueue, DownloadService downloadService, DataService dataService) {
        this.pendingQueue = pendingQueue;
        this.downloadService = downloadService;
        this.dataService = dataService;
    }

    @Scheduled(fixedDelay = 3000)
    private void interval() {
        GlobalState newGlobalState = new GlobalState(
                downloadService.runningCount(),
                pendingQueue.size(),
                dataService.countErrorImages());
        if (!newGlobalState.equals(currentState)) {
            currentState = newGlobalState;
            liveGlobalState.onNext(currentState);
        }
    }
}
