package tn.mnlr.vripper.services;

import io.reactivex.processors.PublishProcessor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.q.DownloadQ;
import tn.mnlr.vripper.q.ExecutionService;

@Service
@EnableScheduling
public class GlobalStateService {

    private final DownloadQ downloadQ;
    private final ExecutionService executionService;
    private final AppStateExchange appStateExchange;

    @Getter
    private GlobalState currentState;

    @Getter
    private PublishProcessor<GlobalState> liveGlobalState = PublishProcessor.create();

    @Autowired
    public GlobalStateService(DownloadQ downloadQ, ExecutionService executionService, AppStateExchange appStateExchange) {
        this.downloadQ = downloadQ;
        this.executionService = executionService;
        this.appStateExchange = appStateExchange;
    }

    @Scheduled(fixedDelay = 3000)
    private void interval() {
        GlobalState newGlobalState = new GlobalState(
                executionService.runningCount(),
                downloadQ.size(),
                appStateExchange.getImages()
                        .values()
                        .stream()
                        .filter(e -> e.getTotal() == 0 || e.getTotal() != e.getCurrent().get())
                        .count(),
                appStateExchange.getImages()
                        .values()
                        .stream()
                        .filter(e -> e.getStatus().equals(Image.Status.ERROR))
                        .count());
        if (!newGlobalState.equals(currentState)) {
            currentState = newGlobalState;
            liveGlobalState.onNext(currentState);
        }
    }
}
